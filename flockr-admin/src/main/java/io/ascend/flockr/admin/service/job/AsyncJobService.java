package io.ascend.flockr.admin.service.job;

import io.ascend.flockr.admin.domain.rule.RuleMeta;
import io.ascend.flockr.admin.domain.rule.SourceInfo;
import io.reactivex.rxjava3.core.Completable;

/**
 * Interface for job execution services.
 *
 * <p>Implementations handle the FULL execution lifecycle:
 *
 * <ul>
 *   <li>Create execution record in database
 *   <li>Update rule status to RUNNING
 *   <li>Submit job to external processing engine
 *   <li>Handle errors and update statuses accordingly
 * </ul>
 *
 * <p>This follows the Strategy Pattern, allowing different execution strategies based on rule type.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
public interface AsyncJobService {

  /**
   * Executes a rule by handling the full lifecycle.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Creates an execution record in the database
   *   <li>Updates rule status to RUNNING
   *   <li>Submits the job to the external processing engine (Spark/Flink)
   *   <li>Handles errors by marking execution and rule as FAILED
   * </ol>
   *
   * @param rule the rule to execute
   * @param triggeredBy who/what triggered this execution (e.g., "scheduler", user email)
   * @return Completable that completes when execution is initiated
   */
  Completable execute(RuleMeta<SourceInfo> rule, String triggeredBy);

  /**
   * Cancels a running job.
   *
   * @param externalJobId the external job reference ID (Spark submissionId or Flink jobId)
   * @return Completable that completes when job is cancelled
   */
  Completable cancelJob(String externalJobId);
}
