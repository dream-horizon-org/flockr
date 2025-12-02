package io.ascend.flockr.admin.domain.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing rule metadata.
 *
 * <p>A rule defines the criteria for determining audience membership. Rules can be either BATCH
 * (SQL-based queries executed periodically) or STREAM (event pattern matching in real-time). This
 * class is generic over the source information type, allowing for both basic {@link SourceInfo} and
 * enriched {@link SourceInfoEnriched} representations.
 *
 * @param <T> the type of source information, extending {@link SourceInfo}
 * @author Prithu Sharma
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleMeta<T extends SourceInfo> {
  /** The tenant identifier that owns this rule. */
  private String tenantId;

  /** The project identifier within the tenant. */
  private String projectId;

  /** The unique identifier of the rule. */
  private Long ruleId;

  /** The identifier of the audience this rule belongs to. */
  private Long audienceId;

  /** The name of the rule. */
  private String name;

  /** A description of the rule's purpose or logic. */
  private String description;

  /** The start time for rule execution (epoch seconds). */
  private Long startTime;

  /** The end time for rule execution (epoch seconds). */
  private Long endTime;

  /** The action to perform when the rule matches (ADD or REMOVE). */
  private RuleAction ruleAction;

  /** The type of rule (BATCH or STREAM). */
  private RuleType ruleType;

  /** The current execution status of the rule. */
  private RuleStatus status;

  /** The rule configuration containing execution details specific to the rule type. */
  private RuleConfiguration<T> configuration;

  /** The username of the user who created this rule. */
  private String createdBy;

  /** Timestamp when the rule was created (epoch seconds). */
  private Long createdAt;

  /** Timestamp when the rule was last updated (epoch seconds). */
  private Long updatedAt;
}
