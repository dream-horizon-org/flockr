package io.ascend.flockr.admin.handlers;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.spark.SparkClient;
import io.ascend.flockr.admin.client.spark.io.response.SparkApplicationInfo;
import io.ascend.flockr.admin.config.ApplicationConfig;
import io.ascend.flockr.admin.domain.rule.JobStatus;
import io.ascend.flockr.admin.domain.rule.RuleExecution;
import io.ascend.flockr.admin.domain.rule.RuleStatus;
import io.ascend.flockr.admin.repository.ExecutionSync;
import io.ascend.flockr.admin.repository.RuleExecutionRepository;
import io.ascend.flockr.admin.repository.RuleRepository;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for reconciling stuck job submissions.
 *
 * <p>This handler periodically finds executions stuck in SUBMITTING status and reconciles them:
 *
 * <ol>
 *   <li>Finds executions with status=SUBMITTING AND created_at < NOW() - 5 minutes
 *   <li>Searches Spark History Server for jobs matching pattern:
 *       "flockr-batch-rule-{ruleId}-exec-{executionId}-*"
 *   <li>If job found: updates execution with actual job details, rule status → RUNNING
 *   <li>If job NOT found: marks execution as FAILED, rule status → FAILED
 * </ol>
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
public non-sealed class ReconcileJobHandler extends AbstractHandler {

  private static final String ERROR_JOB_NOT_FOUND =
      "Job not found in execution engine after 5 minutes";
  private static final int RECONCILE_THRESHOLD_MINUTES = 5;

  private final RuleExecutionRepository ruleExecutionRepository;
  private final RuleRepository ruleRepository;
  private final SparkClient sparkClient;

  @Inject
  public ReconcileJobHandler(
      Vertx vertx,
      ApplicationConfig config,
      ExecutionSync executionSync,
      RuleExecutionRepository ruleExecutionRepository,
      RuleRepository ruleRepository,
      SparkClient sparkClient) {
    super(HandlerState.WAITING_TRIGGER, vertx, executionSync, config.getReconcileJobHandler());
    this.ruleExecutionRepository = ruleExecutionRepository;
    this.ruleRepository = ruleRepository;
    this.sparkClient = sparkClient;
  }

  @Override
  public void handle(Long event) {
    if (!checkAndUpdateState()) {
      log.trace("ReconcileJobHandler not ready, skipping");
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
            count -> log.info("Reconciliation completed. Processed {} stale executions", count),
            error -> log.error("Error during reconciliation", error),
            () -> log.debug("No lease acquired for reconciliation, skipping this cycle"));
  }

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

              return Observable.fromIterable(staleExecutions)
                  .concatMapSingle(this::reconcileExecution)
                  .reduce(0, Integer::sum);
            });
  }

  private Single<Integer> reconcileExecution(RuleExecution execution) {
    log.info(
        "Reconciling execution {} for rule {} (created at {})",
        execution.getExecutionId(),
        execution.getRuleId(),
        execution.getCreatedAt());

    String namePattern = buildJobNamePattern(execution);
    Instant minDate = execution.getCreatedAt();
    Instant maxDate = Instant.now();

    return sparkClient
        .findApplicationsByNamePattern(namePattern, minDate, maxDate)
        .flatMap(
            matchingApps -> {
              if (matchingApps.isEmpty()) {
                return markExecutionAsFailed(execution);
              } else {
                SparkApplicationInfo app = matchingApps.get(0);
                return updateExecutionWithJobDetails(execution, app);
              }
            })
        .map(v -> 1)
        .onErrorResumeNext(
            error -> {
              log.error(
                  "Failed to reconcile execution {} for rule {}",
                  execution.getExecutionId(),
                  execution.getRuleId(),
                  error);
              return Single.just(0);
            });
  }

  /**
   * Builds the job name pattern to search for. Pattern:
   * flockr-batch-rule-{ruleId}-exec-{executionId}-*
   */
  private String buildJobNamePattern(RuleExecution execution) {
    return String.format(
        "flockr-batch-rule-%d-exec-%d-*", execution.getRuleId(), execution.getExecutionId());
  }

  private Single<Void> markExecutionAsFailed(RuleExecution execution) {
    log.warn(
        "Job not found for execution {} (rule {}), marking as FAILED",
        execution.getExecutionId(),
        execution.getRuleId());

    return ruleExecutionRepository
        .markFailed(execution.getExecutionId(), ERROR_JOB_NOT_FOUND)
        .andThen(
            ruleRepository
                .updateRuleStatus(execution.getRuleId(), RuleStatus.FAILED, RuleStatus.SUBMITTING)
                .ignoreElement())
        .toSingleDefault((Void) null);
  }

  private Single<Void> updateExecutionWithJobDetails(
      RuleExecution execution, SparkApplicationInfo app) {

    log.info(
        "Found Spark job {} (state: {}) for execution {}, updating records",
        app.getId(),
        app.getState(),
        execution.getExecutionId());

    JobStatus jobStatus = mapSparkStateToJobStatus(app.getState());
    RuleStatus ruleStatus = mapJobStatusToRuleStatus(jobStatus);

    return ruleExecutionRepository
        .updateStatusAndExternalJobId(
            execution.getExecutionId(), jobStatus, app.getId(), app.getStartTime())
        .andThen(
            ruleRepository
                .updateRuleStatus(execution.getRuleId(), ruleStatus, RuleStatus.SUBMITTING)
                .ignoreElement())
        .toSingleDefault((Void) null);
  }

  private JobStatus mapSparkStateToJobStatus(String sparkState) {
    if (sparkState == null) {
      return JobStatus.SUBMITTED;
    }
    return switch (sparkState.toUpperCase()) {
      case "RUNNING" -> JobStatus.RUNNING;
      case "FINISHED" -> JobStatus.COMPLETED;
      case "FAILED" -> JobStatus.FAILED;
      case "KILLED" -> JobStatus.CANCELLED;
      default -> JobStatus.SUBMITTED;
    };
  }

  private RuleStatus mapJobStatusToRuleStatus(JobStatus jobStatus) {
    return switch (jobStatus) {
      case RUNNING, SUBMITTED, SUBMITTING -> RuleStatus.RUNNING;
      case COMPLETED -> RuleStatus.COMPLETED;
      case FAILED, CANCELLED -> RuleStatus.FAILED;
      case RETRYING -> RuleStatus.RETRYING;
    };
  }
}
