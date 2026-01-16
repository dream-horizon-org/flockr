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
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for reconciling stuck job submissions.
 *
 * <p>Periodically finds executions stuck in SUBMITTING or RUNNING status and reconciles them by
 * checking their actual state in the external execution engine (Spark/Flink).
 *
 * <p><b>Reconciliation flow:</b>
 *
 * <ol>
 *   <li>Find stale SUBMITTING/RUNNING executions (limit 100, using updated_at threshold)
 *   <li>Fetch apps from external engine (single batch API call per job type)
 *   <li>For matched executions: update status if changed (status change prevents re-pickup)
 *   <li>For unmatched SUBMITTING: increment retry count + reclaim, or mark FAILED after max retries
 *   <li>RUNNING executions are checked and updated if status changed
 * </ol>
 *
 * <p><b>Performance optimizations:</b>
 *
 * <ul>
 *   <li>Single Spark/Flink API call instead of N individual calls
 *   <li>Batch database updates grouped by status
 *   <li>No upfront claiming - status changes or retry increment handle concurrency
 *   <li>Skip updates when status unchanged
 * </ul>
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
public non-sealed class ReconcileJobHandler extends AbstractHandler {
  /** Minimum age (in minutes) for an execution to be considered stale. */
  private static final int RECONCILE_THRESHOLD_MINUTES = 5;

  /** Maximum reconciliation retry attempts before marking as FAILED. */
  private static final int MAX_RECONCILIATION_RETRIES = 5;

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
   * Main reconciliation logic - finds and processes all stale executions.
   *
   * @return Single containing count of successfully reconciled executions
   */
  private Single<Integer> reconcileStaleExecutions() {
    return ruleExecutionRepository
        .findStaleExecutionsForReconciliation(RECONCILE_THRESHOLD_MINUTES)
        .flatMap(
            staleExecutions -> {
              if (staleExecutions.isEmpty()) {
                log.info("No stale executions found for reconciliation");
                return Single.just(0);
              }

              // Separate SUBMITTING and RUNNING for logging
              Map<JobStatus, List<RuleExecution>> byCurrentStatus =
                  staleExecutions.stream().collect(Collectors.groupingBy(RuleExecution::getStatus));

              int submittingCount =
                  byCurrentStatus.getOrDefault(JobStatus.SUBMITTING, List.of()).size();
              int runningCount = byCurrentStatus.getOrDefault(JobStatus.RUNNING, List.of()).size();

              log.info(
                  "Found {} stale executions (SUBMITTING: {}, RUNNING: {})",
                  staleExecutions.size(),
                  submittingCount,
                  runningCount);

              return processExecutions(staleExecutions);
            });
  }

  /**
   * Processes all stale executions by fetching external status and updating accordingly.
   *
   * @param executions all stale executions to process
   * @return Single containing count of processed executions
   */
  private Single<Integer> processExecutions(List<RuleExecution> executions) {
    // Build maps for change detection and retry tracking
    Map<Long, JobStatus> currentStatusByExecutionId =
        executions.stream()
            .collect(Collectors.toMap(RuleExecution::getExecutionId, RuleExecution::getStatus));

    Map<Long, Integer> retryCountByExecutionId =
        executions.stream()
            .filter(e -> e.getStatus() == JobStatus.SUBMITTING)
            .collect(
                Collectors.toMap(
                    RuleExecution::getExecutionId,
                    e -> e.getReconciliationRetries() != null ? e.getReconciliationRetries() : 0));

    // Group by rule type for engine-specific API calls
    Map<RuleType, List<RuleExecution>> byType =
        executions.stream()
            .collect(
                Collectors.groupingBy(
                    e -> e.getExecutionType() == JobType.BATCH ? RuleType.BATCH : RuleType.STREAM));

    return Observable.fromIterable(byType.entrySet())
        .concatMapSingle(
            entry ->
                reconcileByRuleType(
                    entry.getKey(),
                    entry.getValue(),
                    currentStatusByExecutionId,
                    retryCountByExecutionId))
        .reduce(0, Integer::sum);
  }

  /**
   * Reconciles executions of a specific rule type using the appropriate service.
   *
   * @param ruleType type of rules to reconcile (BATCH or STREAM)
   * @param executions list of executions of this type
   * @param currentStatusByExecutionId map of execution ID to current status for change detection
   * @param retryCountByExecutionId map of execution ID to retry count for SUBMITTING
   * @return Single containing count of processed executions
   */
  private Single<Integer> reconcileByRuleType(
      RuleType ruleType,
      List<RuleExecution> executions,
      Map<Long, JobStatus> currentStatusByExecutionId,
      Map<Long, Integer> retryCountByExecutionId) {

    log.info("Reconciling {} {} executions", executions.size(), ruleType);

    return ruleExecutionEngineRegistry
        .get(ruleType)
        .fetchAndMatchApplications(executions)
        .flatMap(
            matches ->
                processBatchUpdates(
                    executions, matches, currentStatusByExecutionId, retryCountByExecutionId));
  }

  /**
   * Processes batch updates for matched executions and handles unmatched SUBMITTING.
   *
   * @param executions all executions being processed
   * @param matches list of reconciliation matches from external engine
   * @param currentStatusByExecutionId map of execution ID to current status
   * @param retryCountByExecutionId map of execution ID to retry count for SUBMITTING
   * @return Single containing count of processed executions
   */
  private Single<Integer> processBatchUpdates(
      List<RuleExecution> executions,
      List<ReconciliationMatch> matches,
      Map<Long, JobStatus> currentStatusByExecutionId,
      Map<Long, Integer> retryCountByExecutionId) {

    // Find matched execution IDs
    Set<Long> matchedIds =
        matches.stream().map(ReconciliationMatch::getExecutionId).collect(Collectors.toSet());

    // Find unmatched SUBMITTING executions (need retry handling)
    List<RuleExecution> unmatchedSubmitting =
        executions.stream()
            .filter(e -> e.getStatus() == JobStatus.SUBMITTING)
            .filter(e -> !matchedIds.contains(e.getExecutionId()))
            .toList();

    // Filter matched to only those with status changes
    List<ReconciliationMatch> changedMatches =
        filterMatchesWithStatusChange(matches, currentStatusByExecutionId);

    if (changedMatches.isEmpty() && unmatchedSubmitting.isEmpty()) {
      log.info("No status changes and no unmatched SUBMITTING to process");
      return Single.just(0);
    }

    List<Completable> operations = new ArrayList<>();

    // Handle matched executions with status changes
    if (!changedMatches.isEmpty()) {
      log.info("{} matched executions have status changes", changedMatches.size());

      Map<JobStatus, List<ReconciliationMatch>> matchesByJobStatus =
          groupMatchesByJobStatus(changedMatches);
      Map<RuleStatus, List<Long>> ruleIdsByRuleStatus =
          groupRuleIdsByRuleStatus(matchesByJobStatus);

      operations.addAll(buildExecutionUpdateOperations(matchesByJobStatus));
      operations.addAll(buildRuleUpdateOperations(ruleIdsByRuleStatus));
    }

    // Handle unmatched SUBMITTING (retry or fail)
    if (!unmatchedSubmitting.isEmpty()) {
      operations.add(handleUnmatchedSubmitting(unmatchedSubmitting, retryCountByExecutionId));
    }

    return Completable.merge(operations)
        .toSingle(() -> changedMatches.size() + unmatchedSubmitting.size())
        .onErrorResumeNext(
            error -> {
              log.error("Error during batch updates", error);
              return Single.just(0);
            });
  }

  /**
   * Handles unmatched SUBMITTING executions: increment retry or mark FAILED.
   *
   * @param unmatchedSubmitting list of unmatched SUBMITTING executions
   * @param retryCountByExecutionId map of execution ID to current retry count
   * @return Completable that handles all unmatched executions
   */
  private Completable handleUnmatchedSubmitting(
      List<RuleExecution> unmatchedSubmitting, Map<Long, Integer> retryCountByExecutionId) {

    List<Long> toMarkFailed = new ArrayList<>();
    List<Long> toRetry = new ArrayList<>();

    for (RuleExecution exec : unmatchedSubmitting) {
      int retries = retryCountByExecutionId.getOrDefault(exec.getExecutionId(), 0);
      if (retries >= MAX_RECONCILIATION_RETRIES) {
        log.warn(
            "Execution {} (rule {}) exceeded {} retries, marking as FAILED",
            exec.getExecutionId(),
            exec.getRuleId(),
            MAX_RECONCILIATION_RETRIES);
        toMarkFailed.add(exec.getExecutionId());
      } else {
        log.info(
            "Execution {} (rule {}) not found in external engine, retry {}/{}",
            exec.getExecutionId(),
            exec.getRuleId(),
            retries + 1,
            MAX_RECONCILIATION_RETRIES);
        toRetry.add(exec.getExecutionId());
      }
    }

    String failureReason =
        "Reconciliation failed: Job not found in external engine after "
            + MAX_RECONCILIATION_RETRIES
            + " retries";

    return Completable.mergeArray(
        ruleExecutionRepository.markFailedWithReason(toMarkFailed, failureReason),
        ruleExecutionRepository.incrementRetryCountAndReclaim(toRetry));
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
