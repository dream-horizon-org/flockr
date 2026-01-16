package io.ascend.flockr.admin.domain.rule;

import java.util.Arrays;
import java.util.List;

/**
 * Status of a rule job execution.
 *
 * <p>Tracks the lifecycle of a job from submission through completion or failure.
 *
 * @author Flockr Team
 * @since 1.0
 */
public enum JobStatus {
  /** Job submission in progress. */
  SUBMITTING(true),

  /** Job created, pending submission to Spark/Flink. */
  SUBMITTED(false),

  /** Job running on Spark/Flink. */
  RUNNING(true),

  /** Job completed successfully. */
  COMPLETED(false),

  /** Job failed. */
  FAILED(false),

  /** Job being retried after failure. */
  RETRYING(false),

  /** Job was cancelled. */
  CANCELLED(false);

  private final boolean reconcilable;

  JobStatus(boolean reconcilable) {
    this.reconcilable = reconcilable;
  }

  /**
   * Returns whether this status should be checked during reconciliation.
   *
   * @return true if executions with this status should be reconciled with external engine
   */
  public boolean isReconcilable() {
    return reconcilable;
  }

  /**
   * Returns all statuses that should be reconciled with external engine.
   *
   * @return list of reconcilable statuses
   */
  public static List<JobStatus> getReconcilableStatuses() {
    return Arrays.stream(values()).filter(JobStatus::isReconcilable).toList();
  }
}
