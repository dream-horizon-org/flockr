package io.ascend.flockr.admin.repository;

import io.ascend.flockr.admin.domain.rule.JobStatus;
import io.ascend.flockr.admin.domain.rule.RuleExecution;
import io.ascend.flockr.admin.domain.rule.RuleStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.List;

/**
 * Repository for managing rule executions.
 *
 * <p>This repository handles CRUD operations for the rule_executions table, which tracks individual
 * execution instances of rules.
 *
 * @author Flockr Team
 * @since 1.0
 */
public interface RuleExecutionRepository {
  /**
   * Updates execution status and optionally the external job reference ID.
   *
   * @param executionId the execution ID
   * @param status new status
   * @param externalJobId external job ID (Spark submissionId or Flink jobId), can be null
   * @param startedAt execution start time, can be null
   * @return Completable that completes when update is done
   */
  Completable updateStatusAndExternalJobId(
      Long executionId, JobStatus status, String externalJobId, Instant startedAt);

  /**
   * Marks an execution as failed with error message.
   *
   * @param executionId the execution ID
   * @param errorMessage the error message
   * @return Completable that completes when update is done
   */
  Completable markFailed(Long executionId, String errorMessage);

  Single<Long> createPendingExecutionAndUpdateRuleStatus(
      RuleExecution ruleExecution, Long ruleId, RuleStatus currentStatus, RuleStatus newStatus);

  /**
   * Updates execution details (job name, external ID, status, metadata) and rule status atomically.
   *
   * <p>This is called after successful job submission to update the pending execution with actual
   * job details from Spark/Flink response.
   *
   * @param executionId the execution ID to update
   * @param jobName the job name (includes execution ID for reconciliation)
   * @param externalJobId external job ID from Spark/Flink
   * @param jobStatus new job status
   * @param metadata job metadata (Spark/Flink specific details)
   * @param ruleId the rule ID to update
   * @param newRuleStatus new rule status
   * @return Completable that completes when both updates succeed
   */
  Single<Long> updateExecutionDetailsAndRuleStatus(
      Long executionId,
      String jobName,
      String externalJobId,
      JobStatus jobStatus,
      JsonObject metadata,
      Long ruleId,
      RuleStatus newRuleStatus);

  /**
   * Finds executions stuck in specified statuses for longer than the threshold.
   *
   * <p>Used by the reconciliation process to find jobs that may need status sync with external
   * engine (Spark/Flink).
   *
   * <p><b>Query behavior:</b>
   *
   * <ul>
   *   <li>Filters by status IN (provided statuses) AND updated_at older than threshold
   *   <li>Orders by updated_at ASC (oldest first)
   *   <li>Limited to 100 records per batch to prevent overwhelming the system
   * </ul>
   *
   * @param thresholdMinutes minimum age in minutes for an execution to be considered stale
   * @param statuses list of statuses to query for reconciliation
   * @return Single containing list of stale executions needing reconciliation (max 100)
   */
  Single<List<RuleExecution>> findStaleExecutionsForReconciliation(
      int thresholdMinutes, List<JobStatus> statuses);

  /**
   * Batch update execution status for multiple executions.
   *
   * @param executionIds list of execution IDs to update
   * @param status the status to set for all executions
   * @return Completable that completes when all updates are done
   */
  Completable batchUpdateStatus(List<Long> executionIds, JobStatus status);
}
