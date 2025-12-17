package io.ascend.flockr.admin.client.spark.io.request;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkSubmissionRequest {

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
}
