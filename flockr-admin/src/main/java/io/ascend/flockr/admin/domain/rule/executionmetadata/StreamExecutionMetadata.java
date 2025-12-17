package io.ascend.flockr.admin.domain.rule.executionmetadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Execution metadata for STREAM rules running on Flink.
 *
 * <p>Contains:
 *
 * <ul>
 *   <li>Submit configuration (for debugging/replay)
 *   <li>Savepoint/checkpoint paths (for recovery)
 *   <li>Cluster info (for debugging)
 *   <li>Task summary (lightweight health check)
 *   <li>Restart tracking
 * </ul>
 *
 * <p>Typical JSON size: ~350 bytes
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class StreamExecutionMetadata implements ExecutionMetadata {

  /** Type discriminator for JSON deserialization. */
  @Builder.Default private String type = "STREAM";

  // === Submit Config (for debugging/replay) ===

  /** Flink JAR ID used for submission. */
  private String jarId;

  /** Entry class for the Flink job. */
  private String entryClass;

  /** Configured parallelism for the job. */
  private Integer parallelism;

  // === Savepoint/Recovery ===

  /**
   * Savepoint path this job was resumed from.
   *
   * <p>Null if started fresh.
   */
  private String resumedFromSavepoint;

  /**
   * Latest checkpoint path.
   *
   * <p>Used for recovery if job fails. Updated during status polling.
   */
  private String latestCheckpointPath;

  // === Cluster Info ===

  /** Flink cluster ID where job is running. */
  private String clusterId;

  /** Flink version on the cluster (e.g., "1.17.0"). */
  private String flinkVersion;

  // === Task Summary (lightweight) ===

  /** Total number of tasks in the job. */
  private Integer totalTasks;

  /** Number of currently running tasks. */
  private Integer runningTasks;

  // === Restart Tracking ===

  /**
   * Flink's internal restart count.
   *
   * <p>Helps detect flapping jobs that restart frequently.
   */
  @Builder.Default private Integer restartCount = 0;

  @Override
  public String getType() {
    return type;
  }
}
