package io.ascend.flockr.admin.client.spark.io.request;

import io.ascend.flockr.admin.domain.rule.ExecutableRule;
import io.ascend.flockr.admin.domain.rule.SinkInfoEnriched;
import io.ascend.flockr.admin.domain.rule.SourceInfoEnriched;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Request DTO for Spark REST Submission API (/v1/submissions/create).
 *
 * <p>This follows the Spark Standalone REST API format for submitting applications.
 *
 * @see <a
 *     href="https://spark.apache.org/docs/latest/spark-standalone.html#submitting-applications-to-a-cluster">Spark
 *     Submission API</a>
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkSubmissionRequest {

  /** Main class for Spark batch processing engine. */
  private static final String MAIN_CLASS = "com.dream11.flocker.engine.EngineStart";

  /** Client Spark version. */
  private static final String CLIENT_SPARK_VERSION = "3.5.0";

  /** Action type - always "CreateSubmissionRequest" for job submission. */
  @Builder.Default private String action = "CreateSubmissionRequest";

  /** Application arguments passed to the main class. */
  private List<String> appArgs;

  /** Path to the application JAR (file:// or hdfs://). */
  private String appResource;

  /** Spark version of the client. */
  private String clientSparkVersion;

  /** Environment variables for the application. */
  @Builder.Default private Map<String, String> environmentVariables = Map.of();

  /** Fully qualified main class name. */
  private String mainClass;

  /** Spark configuration properties. */
  private Map<String, String> sparkProperties;

  /**
   * Creates a SparkSubmissionRequest from rule metadata and Spark job configuration.
   *
   * <p>This factory method:
   *
   * <ol>
   *   <li>Builds the job payload JSON from the executable rule
   *   <li>Configures Spark properties (resources, master URL, app name)
   *   <li>Sets up the submission request for the Spark REST API
   * </ol>
   *
   * @param executableRule enriched rule metadata with source and sink details
   * @param sparkJobConfig Spark cluster and resource configuration
   * @param executionId the execution ID for this job
   * @return SparkSubmissionRequest ready for submission
   */
  public static SparkSubmissionRequest fromRule(
      ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> executableRule,
      SparkJobConfig sparkJobConfig,
      Long executionId) {

    log.info(
        "Building Spark submission request for rule {} (execution {})",
        executableRule.getRuleId(),
        executionId);

    // Build the job payload JSON
    JsonObject jobPayload = SparkJobPayloadMapper.buildSparkJobPayload(executableRule);

    // Pass payload as first application argument
    List<String> appArgs = List.of(jobPayload.encode());

    // Configure Spark properties
    Map<String, String> sparkProperties =
        Map.of(
            "spark.master",
            sparkJobConfig.getMasterUrl(),
            "spark.submit.deployMode",
            sparkJobConfig.getDeployMode(),
            "spark.executor.memory",
            sparkJobConfig.getExecutorMemory(),
            "spark.executor.cores",
            String.valueOf(sparkJobConfig.getExecutorCores()),
            "spark.executor.instances",
            String.valueOf(sparkJobConfig.getExecutorInstances()),
            "spark.driver.memory",
            sparkJobConfig.getDriverMemory(),
            "spark.app.name",
            String.format("flockr-batch-rule-%d-exec-%d", executableRule.getRuleId(), executionId));

    // Build the submission request
    SparkSubmissionRequest request =
        SparkSubmissionRequest.builder()
            .action("CreateSubmissionRequest")
            .appResource(sparkJobConfig.getJarPath())
            .clientSparkVersion(CLIENT_SPARK_VERSION)
            .mainClass(MAIN_CLASS)
            .appArgs(appArgs)
            .sparkProperties(sparkProperties)
            .build();

    log.debug("Built Spark submission request: {}", request);
    return request;
  }

  /**
   * Converts this request to a JsonObject for submission to Spark REST API.
   *
   * @return JsonObject representation of this request
   */
  public JsonObject toJsonObject() {
    JsonObject json = new JsonObject();

    json.put("action", action);
    json.put("appResource", appResource);
    json.put("clientSparkVersion", clientSparkVersion);
    json.put("mainClass", mainClass);

    if (appArgs != null && !appArgs.isEmpty()) {
      JsonArray argsArray = new JsonArray();
      appArgs.forEach(argsArray::add);
      json.put("appArgs", argsArray);
    }

    if (environmentVariables != null && !environmentVariables.isEmpty()) {
      JsonObject envVars = new JsonObject();
      environmentVariables.forEach(envVars::put);
      json.put("environmentVariables", envVars);
    }

    if (sparkProperties != null && !sparkProperties.isEmpty()) {
      JsonObject sparkProps = new JsonObject();
      sparkProperties.forEach(sparkProps::put);
      json.put("sparkProperties", sparkProps);
    }

    return json;
  }
}
