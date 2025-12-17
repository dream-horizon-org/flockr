package io.ascend.flockr.admin.domain.rule.executionmetadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Execution metadata for BATCH rules running on Spark.
 *
 * <p>Contains:
 *
 * <ul>
 *   <li>Submit configuration (for debugging/replay)
 *   <li>Spark-specific IDs (for log retrieval)
 *   <li>Version info (for compatibility debugging)
 *   <li>Retry tracking
 * </ul>
 *
 * <p>Typical JSON size: ~280 bytes
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class BatchExecutionMetadata implements ExecutionMetadata {

  /** Type discriminator for JSON deserialization. */
  @Builder.Default private String type = "BATCH";

  // === Submit Config (for debugging/replay) ===

  /** Driver memory configuration (e.g., "1g", "2g"). */
  private String driverMemory;

  /** Executor memory configuration (e.g., "2g", "4g"). */
  private String executorMemory;

  /** Number of cores per executor. */
  private Integer executorCores;

  /** Number of executor instances. */
  private Integer executorInstances;

  // === Spark IDs (essential for log retrieval) ===

  /**
   * Spark Application ID.
   *
   * <p>Different from submissionId. Needed to fetch logs from Spark History Server. Format:
   * "app-20241217-0001"
   */
  private String applicationId;

  /**
   * Worker ID that ran the driver.
   *
   * <p>Useful for debugging node-specific issues.
   */
  private String workerId;

  // === Version Info ===

  /** Spark version on the server (e.g., "3.5.0"). */
  private String serverSparkVersion;

  // === Retry Tracking ===

  /**
   * Attempt number for this execution.
   *
   * <p>Starts at 1, increments on retry.
   */
  @Builder.Default private Integer attemptNumber = 1;

  @Override
  public String getType() {
    return type;
  }
}
