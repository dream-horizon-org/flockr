package io.ascend.flockr.admin.domain.rule;

/**
 * Enumeration of rule execution statuses.
 *
 * <p>Tracks the lifecycle of a rule from creation through execution.
 *
 * @author Flockr Team
 * @since 1.0
 */
public enum RuleStatus {
  /** Rule has been created and is scheduled for execution. */
  SCHEDULED,

  /** Rule is currently being executed. */
  RUNNING,

  /** Rule execution failed due to an error. */
  FAILED,

  /** Rule execution completed successfully. */
  COMPLETED
}
