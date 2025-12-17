package io.ascend.flockr.admin.domain.rule;

/**
 * Status of a rule job execution.
 *
 * <p>Tracks the lifecycle of a job from submission through completion or failure.
 *
 * @author Flockr Team
 * @since 1.0
 */
public enum JobStatus {
  /** Job created, pending submission to Spark/Flink. */
  SUBMITTED,

  /** Job running on Spark/Flink. */
  RUNNING,

  /** Job completed successfully. */
  COMPLETED,

  /** Job failed. */
  FAILED,

  /** Job being retried after failure. */
  RETRYING,

  /** Job was cancelled. */
  CANCELLED
}
