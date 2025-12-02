package io.ascend.flockr.admin.io.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.ascend.flockr.admin.domain.rule.*;
import io.ascend.flockr.admin.validation.ValidRuleTypeConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

/**
 * Request object for creating rules for an audience.
 *
 * <p>This request allows creating one or more rules for a specific audience. Each rule defines
 * criteria for determining audience membership.
 *
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties()
public class CreateRulesRequest {
  /** The ID of the audience to create rules for (set from path parameter). */
  private Long audienceId;

  /** The list of rules to create. Must contain at least one rule. */
  @Valid @NotEmpty private List<Rule> rules;

  /**
   * Represents a single rule definition within the create rules request.
   *
   * <p>Each rule must have a valid configuration that matches its rule type (BATCH or STREAM).
   */
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @ValidRuleTypeConfiguration
  @JsonIgnoreProperties()
  public static class Rule {
    /** The name of the rule. */
    @NotNull private String name;

    /** A description of the rule's purpose or logic. */
    @NotNull private String description;

    /** The start time for rule execution (epoch seconds). */
    @NotNull
    @JsonProperty("start_time")
    private Long startTime;

    /** The end time for rule execution (epoch seconds). */
    @NotNull
    @JsonProperty("end_time")
    private Long endTime;

    /** The type of rule (BATCH or STREAM). */
    @NotNull
    @JsonProperty("rule_type")
    private RuleType ruleType;

    /** The action to perform when the rule matches (ADD or REMOVE). */
    @NotNull
    @JsonProperty("rule_action")
    private RuleAction ruleAction;

    /** The rule configuration containing execution details specific to the rule type. */
    @Valid @NotNull private RuleConfiguration<SourceInfo> configuration;
  }
}
