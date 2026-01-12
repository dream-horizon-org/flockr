package io.ascend.flockr.admin.handlers;

import com.google.inject.Inject;
import io.ascend.flockr.admin.config.ApplicationConfig;
import io.ascend.flockr.admin.domain.rule.JobStatus;
import io.ascend.flockr.admin.domain.rule.JobType;
import io.ascend.flockr.admin.domain.rule.ReconciliationMatch;
import io.ascend.flockr.admin.domain.rule.RuleExecution;
import io.ascend.flockr.admin.domain.rule.RuleStatus;
import io.ascend.flockr.admin.domain.rule.RuleType;
import io.ascend.flockr.admin.repository.ExecutionSync;
import io.ascend.flockr.admin.repository.RuleExecutionRepository;
import io.ascend.flockr.admin.repository.RuleRepository;
import io.ascend.flockr.admin.service.RuleExecutionEngineRegistry;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for reconciling stuck job submissions.
 *
 * <p>Periodically finds executions stuck in SUBMITTING status and reconciles them by checking their
 * actual state in the external execution engine (Spark/Flink).
 *
 * <p><b>Reconciliation flow:</b>
 *
 * <ol>
 *   <li>Find stale SUBMITTING executions (limit 100, using updated_at)
 *   <li>Claim by setting updated_at = NOW() + 1 hour (prevents concurrent processing)
 *   <li>Fetch apps from external engine (single batch API call per job type)
 *   <li>Match and batch update: group by status, execute batch UPDATEs
 *   <li>Unmatched executions are skipped (retried after claim expires in 1 hour)
 * </ol>
 *
 * <p><b>Performance optimizations:</b>
 *
 * <ul>
 *   <li>Single Spark/Flink API call instead of N individual calls
 *   <li>Batch database updates grouped by status
 *   <li>Claim-based locking for distributed safety
 * </ul>
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
public non-sealed class ReconcileJobHandler extends AbstractHandler {
  /** Minimum age (in minutes) for an execution to be considered stale. */
  private static final int RECONCILE_THRESHOLD_MINUTES = 5;

  private final RuleExecutionRepository ruleExecutionRepository;
  private final RuleRepository ruleRepository;
  private final RuleExecutionEngineRegistry ruleExecutionEngineRegistry;

  /**
   * Creates a new ReconcileJobHandler.
   *
   * @param vertx Vert.x instance for async operations
   * @param config application configuration containing handler settings
   * @param executionSync distributed lock mechanism for handler coordination
   * @param ruleExecutionRepository repository for rule execution CRUD operations
   * @param ruleRepository repository for rule CRUD operations
   * @param ruleExecutionEngineRegistry registry that routes to appropriate service by rule type
   */
  @Inject
  public ReconcileJobHandler(
      Vertx vertx,
      ApplicationConfig config,
      ExecutionSync executionSync,
      RuleExecutionRepository ruleExecutionRepository,
      RuleRepository ruleRepository,
      RuleExecutionEngineRegistry ruleExecutionEngineRegistry) {
    super(HandlerState.WAITING_TRIGGER, vertx, executionSync, config.getReconcileJobHandler());
    this.ruleExecutionRepository = ruleExecutionRepository;
    this.ruleRepository = ruleRepository;
    this.ruleExecutionEngineRegistry = ruleExecutionEngineRegistry;
  }

  @Override
  public void handle(Long event) {
    if (!checkAndUpdateState()) {
      log.info("ReconcileJobHandler not ready, skipping");
      return;
    }

    Duration ttl = Duration.ofSeconds(config.getMinExecutionDelay());

    executionSync
        .acquire(config.getSchedulerKey(), ttl)
        .flatMapSingle(
            acquired -> {
              handlerState = HandlerState.RUNNING;
              log.info(
                  "Acquired lease for key={}, starting reconciliation...",
                  config.getSchedulerKey());
              return reconcileStaleExecutions();
            })
        .doFinally(
            () -> {
              handlerState = HandlerState.WAITING_TRIGGER;
              log.debug("ReconcileJobHandler state reset to WAITING_TRIGGER");
            })
        .subscribe(
            count -> log.info("Reconciliation completed. Processed {} executions", count),
            error -> log.error("Error during reconciliation", error),
            () -> log.debug("No lease acquired, skipping this cycle"));
  }

  /**
   * Main reconciliation logic - finds, claims, and processes stale executions.
   *
   * @return Single containing count of successfully reconciled executions
   */
  private Single<Integer> reconcileStaleExecutions() {
    return ruleExecutionRepository
        .findStaleSubmittingExecutions(RECONCILE_THRESHOLD_MINUTES)
        .flatMap(
            staleExecutions -> {
              if (staleExecutions.isEmpty()) {
                log.info("No stale executions found for reconciliation");
                return Single.just(0);
              }

              log.info("Found {} stale executions to reconcile", staleExecutions.size());

              List<Long> executionIds =
                  staleExecutions.stream().map(RuleExecution::getExecutionId).toList();

              return ruleExecutionRepository
                  .claimStaleExecutions(executionIds)
                  .flatMap(claimedIds -> processClaimedExecutions(staleExecutions, claimedIds));
            });
  }

  /**
   * Processes claimed executions by grouping them by rule type and reconciling each group.
   *
   * @param staleExecutions all stale executions found
   * @param claimedIds IDs of successfully claimed executions
   * @return Single containing count of processed executions
   */
  private Single<Integer> processClaimedExecutions(
      List<RuleExecution> staleExecutions, List<Long> claimedIds) {

    if (claimedIds.isEmpty()) {
      log.info("No executions claimed (possibly already claimed by another instance)");
      return Single.just(0);
    }

    log.info("Claimed {} executions for reconciliation", claimedIds.size());

    List<RuleExecution> claimed =
        staleExecutions.stream().filter(e -> claimedIds.contains(e.getExecutionId())).toList();

    Map<RuleType, List<RuleExecution>> byType =
        claimed.stream()
            .collect(
                Collectors.groupingBy(
                    e -> e.getExecutionType() == JobType.BATCH ? RuleType.BATCH : RuleType.STREAM));

    return Observable.fromIterable(byType.entrySet())
        .concatMapSingle(entry -> reconcileByRuleType(entry.getKey(), entry.getValue()))
        .reduce(0, Integer::sum);
  }

  /**
   * Reconciles executions of a specific rule type using the appropriate service.
   *
   * @param ruleType type of rules to reconcile (BATCH or STREAM)
   * @param executions list of executions of this type
   * @return Single containing count of processed executions
   */
  private Single<Integer> reconcileByRuleType(RuleType ruleType, List<RuleExecution> executions) {
    log.info("Reconciling {} {} executions", executions.size(), ruleType);

    return ruleExecutionEngineRegistry
        .get(ruleType)
        .fetchAndMatchApplications(executions)
        .flatMap(this::processBatchUpdates);
  }

  /**
   * Processes batch updates for matched executions.
   *
   * @param matches list of reconciliation matches
   * @return Single containing count of processed executions
   */
  private Single<Integer> processBatchUpdates(List<ReconciliationMatch> matches) {
    if (matches.isEmpty()) {
      log.info("No matched executions to update");
      return Single.just(0);
    }

    Map<JobStatus, List<ReconciliationMatch>> matchesByJobStatus = groupMatchesByJobStatus(matches);
    Map<RuleStatus, List<Long>> ruleIdsByRuleStatus = groupRuleIdsByRuleStatus(matchesByJobStatus);

    List<Completable> operations = new ArrayList<>();
    operations.addAll(buildExecutionUpdateOperations(matchesByJobStatus));
    operations.addAll(buildRuleUpdateOperations(ruleIdsByRuleStatus));

    return Completable.merge(operations)
        .toSingle(() -> matches.size())
        .onErrorResumeNext(
            error -> {
              log.error("Error during batch updates", error);
              return Single.just(0);
            });
  }

  /**
   * Groups matches by their resolved JobStatus.
   *
   * @param matches list of reconciliation matches
   * @return map of JobStatus to list of matches
   */
  private Map<JobStatus, List<ReconciliationMatch>> groupMatchesByJobStatus(
      List<ReconciliationMatch> matches) {

    return matches.stream()
        .collect(
            Collectors.groupingBy(
                m -> mapStateToJobStatus(m.getState()),
                () -> new EnumMap<>(JobStatus.class),
                Collectors.toList()));
  }

  /**
   * Groups rule IDs by their resolved RuleStatus.
   *
   * @param matchesByJobStatus matches grouped by JobStatus
   * @return map of RuleStatus to list of rule IDs
   */
  private Map<RuleStatus, List<Long>> groupRuleIdsByRuleStatus(
      Map<JobStatus, List<ReconciliationMatch>> matchesByJobStatus) {

    Map<RuleStatus, List<Long>> result = new EnumMap<>(RuleStatus.class);

    for (Map.Entry<JobStatus, List<ReconciliationMatch>> entry : matchesByJobStatus.entrySet()) {
      RuleStatus ruleStatus = mapJobStatusToRuleStatus(entry.getKey());
      List<Long> ruleIds = entry.getValue().stream().map(ReconciliationMatch::getRuleId).toList();

      result.computeIfAbsent(ruleStatus, k -> new ArrayList<>()).addAll(ruleIds);
    }

    return result;
  }

  /**
   * Builds batch update operations for executions (one per JobStatus).
   *
   * @param matchesByJobStatus matches grouped by JobStatus
   * @return list of Completable operations
   */
  private List<Completable> buildExecutionUpdateOperations(
      Map<JobStatus, List<ReconciliationMatch>> matchesByJobStatus) {

    List<Completable> operations = new ArrayList<>();

    for (Map.Entry<JobStatus, List<ReconciliationMatch>> entry : matchesByJobStatus.entrySet()) {
      JobStatus status = entry.getKey();
      List<ReconciliationMatch> group = entry.getValue();

      log.info("Batch updating {} executions to status {}", group.size(), status);
      operations.add(ruleExecutionRepository.batchUpdateStatusAndExternalJobId(group, status));
    }

    return operations;
  }

  /**
   * Builds batch update operations for rules (one per RuleStatus).
   *
   * @param ruleIdsByRuleStatus rule IDs grouped by RuleStatus
   * @return list of Completable operations
   */
  private List<Completable> buildRuleUpdateOperations(
      Map<RuleStatus, List<Long>> ruleIdsByRuleStatus) {

    List<Completable> operations = new ArrayList<>();

    for (Map.Entry<RuleStatus, List<Long>> entry : ruleIdsByRuleStatus.entrySet()) {
      RuleStatus status = entry.getKey();
      List<Long> ruleIds = entry.getValue();

      log.info("Batch updating {} rules to status {}", ruleIds.size(), status);
      operations.add(ruleRepository.batchUpdateRuleStatus(ruleIds, status, RuleStatus.SUBMITTING));
    }

    return operations;
  }

  /**
   * Maps external engine state string to internal {@link JobStatus}.
   *
   * @param state the state string from external engine
   * @return corresponding JobStatus
   */
  private JobStatus mapStateToJobStatus(String state) {
    if (state == null) return JobStatus.SUBMITTED;
    return switch (state.toUpperCase()) {
      case "RUNNING" -> JobStatus.RUNNING;
      case "FINISHED", "COMPLETED" -> JobStatus.COMPLETED;
      case "FAILED" -> JobStatus.FAILED;
      case "KILLED", "CANCELED", "CANCELLED" -> JobStatus.CANCELLED;
      default -> JobStatus.SUBMITTED;
    };
  }

  /**
   * Maps internal {@link JobStatus} to {@link RuleStatus}.
   *
   * @param jobStatus the job status
   * @return corresponding rule status
   */
  private RuleStatus mapJobStatusToRuleStatus(JobStatus jobStatus) {
    return switch (jobStatus) {
      case RUNNING, SUBMITTED, SUBMITTING -> RuleStatus.RUNNING;
      case COMPLETED -> RuleStatus.COMPLETED;
      case FAILED, CANCELLED -> RuleStatus.FAILED;
      case RETRYING -> RuleStatus.RETRYING;
    };
  }
}
