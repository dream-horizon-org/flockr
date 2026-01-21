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
  SUBMITTING,

  SUBMITTED,

  RUNNING,

  COMPLETED,

  FAILED,

  CANCELLED
}
