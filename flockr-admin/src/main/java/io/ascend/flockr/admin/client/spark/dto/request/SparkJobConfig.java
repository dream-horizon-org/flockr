package io.ascend.flockr.admin.client.spark.dto.request;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for Spark job submission.
 *
 * <p>Contains Spark cluster settings and resource allocation for job execution.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@Builder
public class SparkJobConfig {

  /** Spark master URL (e.g., spark://localhost:7077). */
  private String masterUrl;

  /** Path to the application JAR. */
  private String jarPath;

  /** Deploy mode: "client" or "cluster". */
  @Builder.Default private String deployMode = "cluster";

  /** Executor memory (e.g., "2g"). */
  @Builder.Default private String executorMemory = "2g";

  /** Number of cores per executor. */
  @Builder.Default private int executorCores = 2;

  /** Number of executor instances. */
  @Builder.Default private int executorInstances = 2;

  /** Driver memory (e.g., "1g"). */
  @Builder.Default private String driverMemory = "1g";
}
