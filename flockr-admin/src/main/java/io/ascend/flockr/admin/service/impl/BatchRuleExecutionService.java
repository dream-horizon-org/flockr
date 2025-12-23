package io.ascend.flockr.admin.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ascend.flockr.admin.client.spark.SparkClient;
import io.ascend.flockr.admin.client.spark.io.request.SparkSubmissionRequest;
import io.ascend.flockr.admin.config.SparkConfig;
import io.ascend.flockr.admin.domain.rule.*;
import io.ascend.flockr.admin.service.RuleExecutionService;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
  private final SparkConfig sparkConfig;

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
   * @param executableRule the rule metadata with enriched source and sink information
   * @param executionId the execution ID for tracking
   * @return Single containing JobSubmissionResult with external job ID and metadata
   */
  @Override
  public Single<JobSubmissionResult> execute(
      ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> executableRule, Long executionId) {

    log.info(
        "Executing BATCH rule {} (execution {}): {}",
        executableRule.getRuleId(),
        executionId,
        executableRule.getName());

    // Build Spark submission request from rule
    SparkSubmissionRequest request =
        SparkSubmissionRequest.fromRule(executableRule, sparkConfig.toJobConfig(), executionId);

    // Submit to Spark cluster
    log.info(
        "Submitting BATCH job to Spark cluster for rule {} (execution {})",
        executableRule.getRuleId(),
        executionId);

    return sparkClient
        .submit(request.toJsonObject())
        .map(response -> buildJobSubmissionResult(executableRule, response, request))
        .doOnSuccess(
            result ->
                log.info(
                    "Successfully submitted BATCH job for rule {} (execution {}). "
                        + "Submission ID: {}, Job Name: {}",
                    executableRule.getRuleId(),
                    executionId,
                    result.getExternalJobId(),
                    result.getJobName()))
        .doOnError(
            error ->
                log.error(
                    "Failed to submit BATCH job for rule {} (execution {}): {}",
                    executableRule.getRuleId(),
                    executionId,
                    error.getMessage(),
                    error));
  }

  /**
   * Builds a JobSubmissionResult from Spark submission response.
   *
   * @param executableRule the rule metadata
   * @param sparkResponse the Spark submission response
   * @param request the original Spark submission request
   * @return JobSubmissionResult containing submission details and metadata
   */
  private JobSubmissionResult buildJobSubmissionResult(
      ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> executableRule,
      io.ascend.flockr.admin.client.spark.io.response.SparkJobSubmissionResponse sparkResponse,
      SparkSubmissionRequest request) {

    java.util.List<Long> sinkIds =
        executableRule.getSinkList() != null
            ? executableRule.getSinkList().stream().map(SinkInfo::getId).toList()
            : java.util.List.of();

    JsonObject metadata =
        new JsonObject()
            .put("type", "BATCH")
            .put("driverMemory", request.getSparkProperties().get("spark.driver.memory"))
            .put("executorMemory", request.getSparkProperties().get("spark.executor.memory"))
            .put(
                "executorCores",
                Integer.parseInt(request.getSparkProperties().get("spark.executor.cores")))
            .put(
                "executorInstances",
                Integer.parseInt(request.getSparkProperties().get("spark.executor.instances")))
            .put("serverSparkVersion", sparkResponse.getServerSparkVersion())
            .put("sinkIds", new JsonArray(sinkIds))
            .put("attemptNumber", 1);

    return JobSubmissionResult.builder()
        .jobName(request.getSparkProperties().get("spark.app.name"))
        .ruleId(executableRule.getRuleId())
        .jobType(JobType.BATCH)
        .externalJobId(sparkResponse.getSubmissionId())
        .initialStatus(JobStatus.SUBMITTED)
        .metadata(metadata)
        .build();
  }
}
