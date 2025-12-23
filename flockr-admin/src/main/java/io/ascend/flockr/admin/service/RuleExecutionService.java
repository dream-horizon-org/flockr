package io.ascend.flockr.admin.service;

import io.ascend.flockr.admin.domain.rule.ExecutableRule;
import io.ascend.flockr.admin.domain.rule.JobSubmissionResult;
import io.ascend.flockr.admin.domain.rule.SinkInfoEnriched;
import io.ascend.flockr.admin.domain.rule.SourceInfoEnriched;
import io.reactivex.rxjava3.core.Single;

/**
 * Interface for job execution services.
 *
 * <p>Implementations handle job submission to external processing engines:
 *
 * <ol>
 *   <li>Update rule status to RUNNING
 *   <li>Submit job to external processing engine (Spark/Flink)
 *   <li>Capture submission response details
 *   <li>Return {@link JobSubmissionResult} for database logging
 * </ol>
 *
 * <p><b>Design Pattern:</b> This follows the Strategy Pattern, allowing different execution
 * strategies based on rule type (BATCH uses Spark, STREAM uses Flink).
 *
 * <p><b>Separation of Concerns:</b> This service focuses on job submission to external engines. The
 * caller (e.g., scheduler) is responsible for logging the result to the {@code rule_executions}
 * table using {@code RuleExecutionRepository}.
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>{@code
 * // 1. Execute rule
 * Single<JobSubmissionResult> resultSingle =
 *     ruleExecutionService.execute(enrichedRule, "scheduler");
 *
 * // 2. Log to database
 * resultSingle.flatMapCompletable(result ->
 *     ruleExecutionRepository.createExecution(
 *         result.getRuleId(),
 *         result.getSinkIds(),
 *         result.getJobType(),
 *         result.getExternalJobId(),
 *         result.getInitialStatus(),
 *         result.getMetadata(),
 *         result.getSubmittedBy()
 *     )
 * );
 * }</pre>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public interface RuleExecutionService {

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
}
