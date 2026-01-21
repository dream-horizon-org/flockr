package io.ascend.flockr.admin.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ascend.flockr.admin.client.spark.SparkClient;
import io.ascend.flockr.admin.client.spark.io.request.SparkSubmissionRequest;
import io.ascend.flockr.admin.client.spark.io.response.SparkApplicationInfo;
import io.ascend.flockr.admin.client.spark.io.response.SparkJobSubmissionResponse;
import io.ascend.flockr.admin.config.SparkConfig;
import io.ascend.flockr.admin.domain.rule.*;
import io.ascend.flockr.admin.service.RuleExecutionEngine;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class BatchRuleExecutionEngine implements RuleExecutionEngine {
  // Spark property keys
  private static final String SPARK_APP_NAME = "spark.app.name";
  private static final String METADATA_SPARK_PROPERTIES = "sparkProperties";
  private static final String METADATA_SINK_IDS = "sinkIds";

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

    // Build Spark submission request from rule TODO make this configurable per as resource tier
    SparkSubmissionRequest request =
        SparkSubmissionRequest.fromRule(executableRule, sparkConfig, executionId);

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
      SparkJobSubmissionResponse sparkResponse,
      SparkSubmissionRequest request) {

    java.util.List<Long> sinkIds =
        executableRule.getSinkList() != null
            ? executableRule.getSinkList().stream().map(SinkInfo::getId).toList()
            : java.util.List.of();

    // Convert Spark properties map to JsonObject
    JsonObject sparkPropertiesJson = new JsonObject();
    request.getSparkProperties().forEach(sparkPropertiesJson::put);

    JsonObject metadata =
        new JsonObject()
            .put(METADATA_SPARK_PROPERTIES, sparkPropertiesJson)
            .put(METADATA_SINK_IDS, new JsonArray(sinkIds));

    return JobSubmissionResult.builder()
        .jobName(request.getSparkProperties().get(SPARK_APP_NAME))
        .ruleId(executableRule.getRuleId())
        .jobType(JobType.BATCH)
        .externalJobId(sparkResponse.getSubmissionId())
        .initialStatus(JobStatus.SUBMITTED)
        .metadata(metadata)
        .build();
  }

  /**
   * {@inheritDoc}
   *
   * <p><b>Spark-specific implementation:</b>
   *
   * <ol>
   *   <li>Calculates date range from earliest execution's created_at to now
   *   <li>Calls Spark History Server API once to fetch all applications in range
   *   <li>Builds a lookup map keyed by application name
   *   <li>Matches each execution to its Spark app using naming convention
   *   <li>Converts matched apps to job-type agnostic {@link ReconciliationMatch}
   * </ol>
   *
   * @see SparkSubmissionRequest#fromRule for job name generation
   */
  @Override
  public Single<List<ReconciliationMatch>> fetchAndMatchApplications(
      List<RuleExecution> executions) {
    if (executions.isEmpty()) {
      return Single.just(List.of());
    }

    Instant minDate =
        executions.stream()
            .map(RuleExecution::getCreatedAt)
            .map(instant -> instant.minus(Duration.ofHours(1)))
            .min(Instant::compareTo)
            .orElse(Instant.now().minus(Duration.ofDays(1)));
    Instant maxDate = Instant.now();

    log.info(
        "Fetching Spark applications from {} to {} for {} executions",
        minDate,
        maxDate,
        executions.size());

    return sparkClient
        .listApplications(null, minDate, maxDate, null)
        .map(sparkApps -> matchExecutionsToApps(executions, sparkApps));
  }

  /**
   * Matches executions to Spark applications/drivers and converts to ReconciliationMatch.
   *
   * <p>Supports both client mode (apps with names) and cluster mode (drivers matched by ID).
   *
   * @param executions list of executions to match
   * @param sparkApps list of Spark applications/drivers fetched from Master
   * @return list of matched executions (unmatched are excluded)
   */
  private List<ReconciliationMatch> matchExecutionsToApps(
      List<RuleExecution> executions, List<SparkApplicationInfo> sparkApps) {

    log.info("Fetched {} Spark applications/drivers from Master", sparkApps.size());

    // Build lookup map: applicationName -> SparkApplicationInfo (for client mode apps)
    Map<String, SparkApplicationInfo> appByName = new HashMap<>();
    // Build lookup map: driverId -> SparkApplicationInfo (for cluster mode drivers)
    Map<String, SparkApplicationInfo> appById = new HashMap<>();

    for (SparkApplicationInfo app : sparkApps) {
      if (app.getName() != null) {
        appByName.put(app.getName(), app);
      }
      if (app.getId() != null) {
        appById.put(app.getId(), app);
      }
    }

    // Match and convert to ReconciliationMatch
    List<ReconciliationMatch> matches = new ArrayList<>();
    int unmatchedCount = 0;

    for (RuleExecution execution : executions) {
      SparkApplicationInfo app = findMatchingApp(execution, appByName, appById);

      if (app != null) {
        matches.add(
            ReconciliationMatch.builder()
                .executionId(execution.getExecutionId())
                .ruleId(execution.getRuleId())
                .externalJobId(app.getId())
                .state(app.getState())
                .startedAt(app.getStartTime())
                .build());

        log.debug(
            "Matched execution {} to Spark app/driver '{}' (id: {}, state: {})",
            execution.getExecutionId(),
            app.getName(),
            app.getId(),
            app.getState());
      } else {
        unmatchedCount++;
        log.debug(
            "No matching Spark app/driver found for execution {} (rule {}, externalJobId: {})",
            execution.getExecutionId(),
            execution.getRuleId(),
            execution.getExternalJobId());
      }
    }

    log.info(
        "Reconciliation matching complete: {} matched, {} unmatched",
        matches.size(),
        unmatchedCount);

    return matches;
  }

  /**
   * Finds a matching Spark application or driver for the given execution.
   *
   * <p>Matching strategy:
   *
   * <ol>
   *   <li>First tries to match by expected app name: {@code
   *       flockr-batch-rule-{ruleId}-exec-{executionId}} (works for client mode apps that have
   *       spark.app.name set)
   *   <li>Falls back to matching by externalJobId/driverId (works for cluster mode drivers)
   * </ol>
   *
   * @param execution the execution to find a match for
   * @param appByName map of application name to application info
   * @param appById map of application/driver ID to application info
   * @return matching SparkApplicationInfo, or null if not found
   */
  private SparkApplicationInfo findMatchingApp(
      RuleExecution execution,
      Map<String, SparkApplicationInfo> appByName,
      Map<String, SparkApplicationInfo> appById) {

    // First try: match by expected app name (client mode)
    String expectedName =
        String.format(
            "flockr-batch-rule-%d-exec-%d", execution.getRuleId(), execution.getExecutionId());

    SparkApplicationInfo app = appByName.get(expectedName);
    if (app != null) {
      return app;
    }

    // Second try: match by externalJobId (cluster mode drivers)
    String externalJobId = execution.getExternalJobId();
    if (externalJobId != null) {
      return appById.get(externalJobId);
    }

    return null;
  }
}
