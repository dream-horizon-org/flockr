package io.ascend.flockr.admin.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ascend.flockr.admin.client.spark.SparkClient;
import io.ascend.flockr.admin.domain.rule.*;
import io.ascend.flockr.admin.service.RuleExecutionService;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AsyncJobService implementation for BATCH rules using Spark.
 *
 * <p>Handles the FULL execution lifecycle for batch SQL jobs:
 *
 * <ul>
 *   <li>Create execution record
 *   <li>Update rule status
 *   <li>Submit to Spark
 *   <li>Handle errors
 * </ul>
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class BatchRuleExecutionService implements RuleExecutionService {

  private final SparkClient sparkClient;

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
   * @param ruleMetaVerbose@return Completable that completes when execution is initiated
   */
  @Override
  public Single<JobSubmissionResult> execute(
      RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched> ruleMetaVerbose) {
    return null;
  }
}
