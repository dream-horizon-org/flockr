package io.ascend.flockr.admin.domain.rule;

import io.ascend.flockr.admin.domain.rule.executionmetadata.ExecutionMetadata;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of submitting a job to an external execution engine (Spark/Flink).
 *
 * <p>This is the contract between execution services and the caller, containing all information
 * needed to:
 *
 * <ul>
 *   <li>Create a {@link RuleExecution} record in the database
 *   <li>Track the job in the external system
 *   <li>Retrieve logs and monitor status
 * </ul>
 *
 * <p>Design pattern: This follows the Result Object pattern, encapsulating all submission details
 * in a type-safe, engine-agnostic way.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSubmissionResult {

  /** Human-readable name assigned to the job for identification and monitoring. */
  private String jobName;

  /** The rule ID that was executed. */
  private Long ruleId;

  /** List of sink IDs where output will be sent. */
  private List<Long> sinkIds;

  /** Type of job (BATCH or STREAM). */
  private JobType jobType;

  /**
   * External job identifier from the execution engine.
   *
   * <ul>
   *   <li>For Spark: submissionId (e.g., "driver-20241217-1234-0001")
   *   <li>For Flink: jobId (e.g., "a1b2c3d4e5f6...")
   * </ul>
   *
   * <p>This ID is used to query job status and retrieve logs from the external system.
   */
  private String externalJobId;

  /**
   * Initial status of the job after submission.
   *
   * <p>Typically {@link JobStatus#SUBMITTED} or {@link JobStatus#RUNNING} depending on the
   * execution engine's behavior.
   */
  private JobStatus initialStatus;

  /**
   * Engine-specific execution metadata.
   *
   * <p>This is a sealed interface that can be:
   *
   * <ul>
   *   <li>{@link io.ascend.flockr.admin.domain.rule.executionmetadata.BatchExecutionMetadata} - for
   *       Spark jobs (contains applicationId, workerId, Spark version, etc.)
   *   <li>{@link io.ascend.flockr.admin.domain.rule.executionmetadata.StreamExecutionMetadata} -
   *       for Flink jobs (contains jarId, parallelism, checkpoint paths, etc.)
   * </ul>
   *
   * <p>This metadata will be serialized to JSONB in the database for debugging and monitoring.
   */
  private ExecutionMetadata metadata;
}
