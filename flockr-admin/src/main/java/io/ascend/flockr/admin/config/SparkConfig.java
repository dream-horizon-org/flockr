package io.ascend.flockr.admin.config;

import com.typesafe.config.Optional;
import io.ascend.flockr.admin.client.spark.dto.request.SparkJobConfig;
import io.ascend.flockr.admin.config.provider.ConfigProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for Spark client and job submission.
 *
 * <p>Contains both HTTP client settings for communicating with the Spark REST API and job
 * submission settings like resource allocation.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
public class SparkConfig {
  // HTTP Client defaults
  private static final String DEFAULT_HOST = "localhost";
  private static final Integer DEFAULT_PORT = 6066;
  private static final Integer DEFAULT_CONNECTION_TIMEOUT = 30000;
  private static final Integer DEFAULT_REQUEST_TIMEOUT = 60000;
  private static final Integer DEFAULT_MAX_RETRIES = 3;

  // Job submission defaults
  private static final String DEFAULT_MASTER_URL = "spark://localhost:7077";
  private static final String DEFAULT_JAR_PATH =
      "file:///opt/spark/jars/flockr-historic-engine.jar";
  private static final String DEFAULT_DEPLOY_MODE = "cluster";
  private static final String DEFAULT_EXECUTOR_MEMORY = "2g";
  private static final Integer DEFAULT_EXECUTOR_CORES = 2;
  private static final Integer DEFAULT_EXECUTOR_INSTANCES = 2;
  private static final String DEFAULT_DRIVER_MEMORY = "1g";

  // HTTP Client configuration
  @Optional private String host = DEFAULT_HOST;
  @Optional private int port = DEFAULT_PORT;
  @Optional private int connectTimeout = DEFAULT_CONNECTION_TIMEOUT;
  @Optional private int requestTimeout = DEFAULT_REQUEST_TIMEOUT;
  @Optional private int maxRetries = DEFAULT_MAX_RETRIES;

  // Job submission configuration
  @Optional private String masterUrl = DEFAULT_MASTER_URL;
  @Optional private String jarPath = DEFAULT_JAR_PATH;
  @Optional private String deployMode = DEFAULT_DEPLOY_MODE;
  @Optional private String executorMemory = DEFAULT_EXECUTOR_MEMORY;
  @Optional private int executorCores = DEFAULT_EXECUTOR_CORES;
  @Optional private int executorInstances = DEFAULT_EXECUTOR_INSTANCES;
  @Optional private String driverMemory = DEFAULT_DRIVER_MEMORY;

  public static ConfigProvider<SparkConfig> provider() {
    return new ConfigProvider<>("spark", SparkConfig.class);
  }

  /** Gets the base URL for the Spark REST API. */
  public String getBaseUrl() {
    return String.format("http://%s:%d", host, port);
  }

  /**
   * Builds a SparkJobConfig from this configuration.
   *
   * <p>Used by BatchJobService to create job submission requests.
   *
   * @return SparkJobConfig with current settings
   */
  public SparkJobConfig toJobConfig() {
    return SparkJobConfig.builder()
        .masterUrl(masterUrl)
        .jarPath(jarPath)
        .deployMode(deployMode)
        .executorMemory(executorMemory)
        .executorCores(executorCores)
        .executorInstances(executorInstances)
        .driverMemory(driverMemory)
        .build();
  }
}
