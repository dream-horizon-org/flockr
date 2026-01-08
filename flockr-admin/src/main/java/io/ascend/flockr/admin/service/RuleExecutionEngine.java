package io.ascend.flockr.admin.service;

import io.ascend.flockr.admin.domain.rule.*;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface RuleExecutionEngine {

  /**
   * Executes a rule by submitting it to the appropriate processing engine.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Updates rule status to RUNNING
   *   <li>Submits job to Spark (BATCH) or Flink (STREAM)
   *   <li>Captures external job ID and metadata
   *   <li>Returns submission details for database logging
   * </ul>
   *
   * @param executableRule the enriched rule containing sources and sinks
   * @param executionId the execution ID for tracking
   * @return Single emitting {@link JobSubmissionResult} containing:
   *     <ul>
   *       <li>External job ID (submissionId/jobId)
   *       <li>Initial status (SUBMITTED/RUNNING)
   *       <li>Engine-specific metadata as JsonObject (flexible, engine-agnostic)
   *       <li>Rule ID with sink IDs embedded in metadata
   *     </ul>
   */
  Single<JobSubmissionResult> execute(
      ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> executableRule, Long executionId);

  /**
   * Fetches application/job info from the external engine and matches them to executions.
   *
   * <p>Used for reconciliation of stale executions. Makes a <b>single batch API call</b> to the
   * external engine (Spark/Flink) and matches fetched jobs to the provided executions.
   *
   * <p><b>Performance:</b> Instead of N API calls, this method fetches all applications in the
   * relevant date range with one call and performs matching in-memory using the job naming
   * convention: {@code flockr-batch-rule-{ruleId}-exec-{executionId}}
   *
   * <p><b>Returns only matched executions.</b> Unmatched executions are excluded and will be
   * retried in subsequent reconciliation cycles after their claim expires.
   *
   * @param executions list of stale executions to reconcile
   * @return Single containing list of matched executions with their external job info
   */
  Single<List<ReconciliationMatch>> fetchAndMatchApplications(List<RuleExecution> executions);
}
