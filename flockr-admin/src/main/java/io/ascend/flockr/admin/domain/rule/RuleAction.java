package io.ascend.flockr.admin.domain.rule;

/**
 * Enumeration of actions that a rule can perform when matched.
 *
 * <p>Rules define criteria for determining audience membership. When a rule matches, it performs
 * one of these actions on the user's membership in the audience.
 *
 * @author Flockr Team
 * @since 1.0
 */
public enum RuleAction {
  /** Add the matched user(s) to the audience. */
  ADD,

  /** Remove the matched user(s) from the audience. */
  REMOVE
}
