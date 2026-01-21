package io.ascend.flockr.admin.client.spark.io.request;

import io.ascend.flockr.admin.config.SparkConfig;
import io.ascend.flockr.admin.domain.rule.ExecutableRule;
import io.ascend.flockr.admin.domain.rule.SinkInfoEnriched;
import io.ascend.flockr.admin.domain.rule.SourceInfoEnriched;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
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
  private static final String MAIN_CLASS = "io.ascend.flockr.engine.EngineStart";

  /** Client Spark version. */
  private static final String CLIENT_SPARK_VERSION = "4.0.1";

  /** Action type for Spark submission. */
  private static final String ACTION_CREATE_SUBMISSION = "CreateSubmissionRequest";

  // Spark property keys
  private static final String SPARK_MASTER = "spark.master";
  private static final String SPARK_DEPLOY_MODE = "spark.submit.deployMode";
  private static final String SPARK_EXECUTOR_MEMORY = "spark.executor.memory";
  private static final String SPARK_EXECUTOR_CORES = "spark.executor.cores";
  private static final String SPARK_EXECUTOR_INSTANCES = "spark.executor.instances";
  private static final String SPARK_DRIVER_MEMORY = "spark.driver.memory";
  private static final String SPARK_APP_NAME = "spark.app.name";

  // App name format
  private static final String APP_NAME_FORMAT = "flockr-batch-rule-%d-exec-%d";

  // JSON field keys
  private static final String JSON_ACTION = "action";
  private static final String JSON_APP_RESOURCE = "appResource";
  private static final String JSON_CLIENT_SPARK_VERSION = "clientSparkVersion";
  private static final String JSON_MAIN_CLASS = "mainClass";
  private static final String JSON_APP_ARGS = "appArgs";
  private static final String JSON_ENVIRONMENT_VARIABLES = "environmentVariables";
  private static final String JSON_SPARK_PROPERTIES = "sparkProperties";

  /** Action type - always "CreateSubmissionRequest" for job submission. */
  private String action = ACTION_CREATE_SUBMISSION;

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
   * Creates a SparkSubmissionRequest from rule metadata and Spark configuration.
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
   * @param sparkConfig Spark cluster and resource configuration
   * @param executionId the execution ID for this job
   * @return SparkSubmissionRequest ready for submission
   */
  public static SparkSubmissionRequest fromRule(
      ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> executableRule,
      SparkConfig sparkConfig,
      Long executionId) {

    log.info(
        "Building Spark submission request for rule {} (execution {})",
        executableRule.getRuleId(),
        executionId);

    JsonObject jobPayload = SparkJobPayloadMapper.buildSparkJobPayload(executableRule);

    List<String> appArgs = List.of(jobPayload.encode());

    // Build Spark properties
    Map<String, String> sparkProperties = new HashMap<>();
    sparkProperties.put(SPARK_MASTER, sparkConfig.getMasterUrl());
    sparkProperties.put(SPARK_DEPLOY_MODE, sparkConfig.getDeployMode());
    sparkProperties.put(SPARK_EXECUTOR_MEMORY, sparkConfig.getExecutorMemory());
    sparkProperties.put(SPARK_EXECUTOR_CORES, String.valueOf(sparkConfig.getExecutorCores()));
    sparkProperties.put(
        SPARK_EXECUTOR_INSTANCES, String.valueOf(sparkConfig.getExecutorInstances()));
    sparkProperties.put(SPARK_DRIVER_MEMORY, sparkConfig.getDriverMemory());
    sparkProperties.put(
        SPARK_APP_NAME, String.format(APP_NAME_FORMAT, executableRule.getRuleId(), executionId));

    // Build the submission request
    SparkSubmissionRequest request =
        SparkSubmissionRequest.builder()
            .action(ACTION_CREATE_SUBMISSION)
            .appResource(sparkConfig.getJarPath())
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

    json.put(JSON_ACTION, action);
    json.put(JSON_APP_RESOURCE, appResource);
    json.put(JSON_CLIENT_SPARK_VERSION, clientSparkVersion);
    json.put(JSON_MAIN_CLASS, mainClass);

    if (appArgs != null && !appArgs.isEmpty()) {
      JsonArray argsArray = new JsonArray();
      appArgs.forEach(argsArray::add);
      json.put(JSON_APP_ARGS, argsArray);
    }

    JsonObject envVars = new JsonObject();
    environmentVariables.forEach(envVars::put);
    json.put(JSON_ENVIRONMENT_VARIABLES, envVars);

    if (sparkProperties != null && !sparkProperties.isEmpty()) {
      JsonObject sparkProps = new JsonObject();
      sparkProperties.forEach(sparkProps::put);
      json.put(JSON_SPARK_PROPERTIES, sparkProps);
    }

    return json;
  }
}
