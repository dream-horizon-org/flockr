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
 * Handler for reconciling job statuses with external execution engine.
 *
 * <p>Periodically finds executions in reconcilable statuses (configured via {@link JobStatus}) and
 * syncs their status with the external execution engine (Spark/Flink).
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
public non-sealed class ReconcileJobHandler extends AbstractHandler {
  /** Minimum age (in minutes) for an execution to be considered stale. */
  private static final int RECONCILE_THRESHOLD_MINUTES = 1;

  private final RuleExecutionRepository ruleExecutionRepository;
  private final RuleRepository ruleRepository;
  private final RuleExecutionEngineRegistry ruleExecutionEngineRegistry;
  private final List<JobStatus> reconcilableStatuses =
      List.of(JobStatus.SUBMITTED, JobStatus.RUNNING);

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
   * Main reconciliation logic - finds and processes all stale executions.
   *
   * @return Single containing count of successfully reconciled executions
   */
  private Single<Integer> reconcileStaleExecutions() {
    return ruleExecutionRepository
        .findStaleExecutionsForReconciliation(
            RECONCILE_THRESHOLD_MINUTES, this.reconcilableStatuses)
        .flatMap(this::processExecutions);
  }

  /**
   * Processes all stale executions by fetching external status and updating accordingly.
   *
   * @param executions all stale executions to process
   * @return Single containing count of processed executions
   */
  private Single<Integer> processExecutions(List<RuleExecution> executions) {
    if (executions.isEmpty()) {
      log.info("No stale executions found for reconciliation");
      return Single.just(0);
    }

    // Log breakdown by status
    Map<JobStatus, Long> countByStatus =
        executions.stream()
            .collect(Collectors.groupingBy(RuleExecution::getStatus, Collectors.counting()));

    log.info("Found {} stale executions to reconcile: {}", executions.size(), countByStatus);

    // Build map for change detection
    Map<Long, JobStatus> currentStatusByExecutionId =
        executions.stream()
            .collect(Collectors.toMap(RuleExecution::getExecutionId, RuleExecution::getStatus));

    // Group by rule type for engine-specific API calls
    Map<RuleType, List<RuleExecution>> byType =
        executions.stream()
            .collect(
                Collectors.groupingBy(
                    e -> e.getExecutionType() == JobType.BATCH ? RuleType.BATCH : RuleType.STREAM));

    return Observable.fromIterable(byType.entrySet())
        .concatMapSingle(
            entry ->
                reconcileByRuleType(entry.getKey(), entry.getValue(), currentStatusByExecutionId))
        .reduce(0, Integer::sum);
  }

  /**
   * Reconciles executions of a specific rule type using the appropriate service.
   *
   * @param ruleType type of rules to reconcile (BATCH or STREAM)
   * @param executions list of executions of this type
   * @param currentStatusByExecutionId map of execution ID to current status for change detection
   * @return Single containing count of processed executions
   */
  private Single<Integer> reconcileByRuleType(
      RuleType ruleType,
      List<RuleExecution> executions,
      Map<Long, JobStatus> currentStatusByExecutionId) {

    log.info("Reconciling {} {} executions", executions.size(), ruleType);

    return ruleExecutionEngineRegistry
        .get(ruleType)
        .fetchAndMatchApplications(executions)
        .flatMap(matches -> processBatchUpdates(matches, currentStatusByExecutionId));
  }

  /**
   * Processes batch updates for matched executions where status has changed.
   *
   * @param matches list of reconciliation matches from external engine
   * @param currentStatusByExecutionId map of execution ID to current status
   * @return Single containing count of processed executions
   */
  private Single<Integer> processBatchUpdates(
      List<ReconciliationMatch> matches, Map<Long, JobStatus> currentStatusByExecutionId) {

    if (matches.isEmpty()) {
      log.info("No matched executions found in external engine");
      return Single.just(0);
    }

    // Filter to only those with status changes
    List<ReconciliationMatch> changedMatches =
        filterMatchesWithStatusChange(matches, currentStatusByExecutionId);

    if (changedMatches.isEmpty()) {
      log.info("No status changes detected for {} matched executions", matches.size());
      return Single.just(0);
    }

    log.info(
        "{} of {} matched executions have status changes to process",
        changedMatches.size(),
        matches.size());

    Map<JobStatus, List<ReconciliationMatch>> matchesByJobStatus =
        groupMatchesByJobStatus(changedMatches);
    Map<RuleStatus, List<Long>> ruleIdsByRuleStatus = groupRuleIdsByRuleStatus(matchesByJobStatus);

    List<Completable> operations = new ArrayList<>();
    operations.addAll(buildExecutionUpdateOperations(matchesByJobStatus));
    operations.addAll(buildRuleUpdateOperations(ruleIdsByRuleStatus));

    return Completable.merge(operations)
        .toSingle(changedMatches::size)
        .onErrorResumeNext(
            error -> {
              log.error("Error during batch updates", error);
              return Single.just(0);
            });
  }

  /**
   * Filters matches to only those where external status differs from current DB status.
   *
   * @param matches list of reconciliation matches
   * @param currentStatusByExecutionId map of execution ID to current status
   * @return filtered list of matches with actual status changes
   */
  private List<ReconciliationMatch> filterMatchesWithStatusChange(
      List<ReconciliationMatch> matches, Map<Long, JobStatus> currentStatusByExecutionId) {

    return matches.stream()
        .filter(
            match -> {
              JobStatus current = currentStatusByExecutionId.get(match.getExecutionId());
              if (current == null) {
                log.warn(
                    "No current status found for execution {}, skipping", match.getExecutionId());
                return false;
              }
              JobStatus resolved = mapStateToJobStatus(match.getState());
              boolean changed = resolved != current;
              if (!changed) {
                log.debug(
                    "Skipping execution {} - status unchanged (current: {}, external: {})",
                    match.getExecutionId(),
                    current,
                    resolved);
              }
              return changed;
            })
        .toList();
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
      List<Long> executionIds = group.stream().map(ReconciliationMatch::getExecutionId).toList();

      log.info("Batch updating {} executions to status {}", executionIds.size(), status);
      operations.add(ruleExecutionRepository.batchUpdateStatus(executionIds, status));
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
      case FAILED -> RuleStatus.FAILED;
      case CANCELLED -> RuleStatus.CANCELLED;
    };
  }
}
