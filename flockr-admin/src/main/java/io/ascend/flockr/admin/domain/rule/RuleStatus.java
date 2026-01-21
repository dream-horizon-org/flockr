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
  SCHEDULED,

  SUBMITTING,

  RUNNING,

  FAILED,

  COMPLETED,

  CANCELLED
}
