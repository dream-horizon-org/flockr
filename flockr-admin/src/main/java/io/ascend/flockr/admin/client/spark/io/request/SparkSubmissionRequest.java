package io.ascend.flockr.admin.client.spark.io.request;

import io.ascend.flockr.admin.config.SparkConfig;
import io.ascend.flockr.admin.domain.rule.ExecutableRule;
import io.ascend.flockr.admin.domain.rule.SinkInfoEnriched;
import io.ascend.flockr.admin.domain.rule.SourceInfoEnriched;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
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
  private static final String CLIENT_SPARK_VERSION = "3.5.3";

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
  private static final String SPARK_DRIVER_EXTRA_JAVA_OPTIONS = "spark.driver.extraJavaOptions";
  private static final String SPARK_EXECUTOR_EXTRA_JAVA_OPTIONS = "spark.executor.extraJavaOptions";

  /**
   * Returns Java 17+ module system compatibility options required for Spark 3.5.3.
   *
   * <p>These options allow Spark to access internal JDK classes that are not exported by default
   * in Java 9+ module system. Required to prevent IllegalAccessError when Spark accesses classes
   * like sun.nio.ch.DirectBuffer.
   *
   * @return Space-separated string of --add-opens JVM arguments
   */
  private static String getJava17SparkOptions() {
    List<String> options = new ArrayList<>();
    options.add("--add-opens java.base/sun.nio.ch=ALL-UNNAMED");
    options.add("--add-opens java.base/java.lang=ALL-UNNAMED");
    options.add("--add-opens java.base/java.lang.reflect=ALL-UNNAMED");
    options.add("--add-opens java.base/java.lang.invoke=ALL-UNNAMED");
    options.add("--add-opens java.base/java.util=ALL-UNNAMED");
    options.add("--add-opens java.base/java.util.concurrent=ALL-UNNAMED");
    options.add("--add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED");
    options.add("--add-opens java.base/java.io=ALL-UNNAMED");
    options.add("--add-opens java.base/java.nio=ALL-UNNAMED");
    options.add("--add-opens java.base/java.net=ALL-UNNAMED");
    options.add("--add-opens java.base/java.text=ALL-UNNAMED");
    options.add("--add-opens java.desktop/java.awt.font=ALL-UNNAMED");
    return String.join(" ", options);
  }

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

    // Build Spark properties with Java 17 compatibility options
    Map<String, String> sparkProperties = new HashMap<>();
    sparkProperties.put(SPARK_MASTER, sparkConfig.getMasterUrl());
    sparkProperties.put(SPARK_DEPLOY_MODE, sparkConfig.getDeployMode());
    sparkProperties.put(SPARK_EXECUTOR_MEMORY, sparkConfig.getExecutorMemory());
    sparkProperties.put(SPARK_EXECUTOR_CORES, String.valueOf(sparkConfig.getExecutorCores()));
    sparkProperties.put(SPARK_EXECUTOR_INSTANCES, String.valueOf(sparkConfig.getExecutorInstances()));
    sparkProperties.put(SPARK_DRIVER_MEMORY, sparkConfig.getDriverMemory());
    sparkProperties.put(SPARK_APP_NAME, String.format(APP_NAME_FORMAT, executableRule.getRuleId(), executionId));
    
    // Add Java 17+ module system compatibility options for both driver and executor
    String java17Options = getJava17SparkOptions();
    sparkProperties.put(SPARK_DRIVER_EXTRA_JAVA_OPTIONS, java17Options);
    sparkProperties.put(SPARK_EXECUTOR_EXTRA_JAVA_OPTIONS, java17Options);

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
