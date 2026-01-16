package io.ascend.flockr.admin.repository;

import io.ascend.flockr.admin.domain.rule.JobStatus;
import io.ascend.flockr.admin.domain.rule.ReconciliationMatch;
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
   * Finds executions stuck in SUBMITTING or RUNNING status for longer than the threshold.
   *
   * <p>Used by the reconciliation process to find jobs that may have been submitted but whose
   * response was lost, or jobs that are running but may have completed/failed externally.
   *
   * <p><b>Query behavior:</b>
   *
   * <ul>
   *   <li>Filters by status IN (SUBMITTING, RUNNING) AND updated_at older than threshold
   *   <li>Orders by updated_at ASC (oldest first)
   *   <li>Limited to 100 records per batch to prevent overwhelming the system
   * </ul>
   *
   * @param thresholdMinutes minimum age in minutes for an execution to be considered stale
   * @return Single containing list of stale executions needing reconciliation (max 100)
   */
  Single<List<RuleExecution>> findStaleExecutionsForReconciliation(int thresholdMinutes);

  /**
   * Increments reconciliation retry count for unmatched executions and reclaims them. Sets
   * updated_at = NOW() + 1 minute for retry.
   *
   * @param executionIds list of unmatched execution IDs
   * @return Completable that completes when update is done
   */
  Completable incrementRetryCountAndReclaim(List<Long> executionIds);

  /**
   * Marks executions as FAILED after exhausting reconciliation retries. Stores failure reason in
   * metadata.
   *
   * @param executionIds list of execution IDs to mark failed
   * @param failureReason reason for failure to store in metadata
   * @return Completable that completes when update is done
   */
  Completable markFailedWithReason(List<Long> executionIds, String failureReason);

  /**
   * Batch update execution status and external job details for reconciled executions.
   *
   * <p>Uses PostgreSQL UNNEST for efficient multi-row updates. Each execution gets its own
   * external_job_id and started_at.
   *
   * @param matches list of reconciliation matches containing execution and job details
   * @param status the status to set for all executions
   * @return Completable that completes when all updates are done
   */
  Completable batchUpdateStatusAndExternalJobId(
      List<ReconciliationMatch> matches, JobStatus status);
}
