package io.ascend.flockr.admin.io.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.ascend.flockr.admin.domain.rule.*;
import io.ascend.flockr.admin.validation.ValidFutureEpoch;
import io.ascend.flockr.admin.validation.ValidRuleTypeConfiguration;
import io.ascend.flockr.admin.validation.ValidTimeRange;
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateRulesRequest {
  /** The ID of the audience to create rules for (set from path parameter). */
  private Long audienceId;

  /** The list of rules to create. Must contain at least one rule. */
  @Valid @NotEmpty private List<Rule> rules;

  /**
   * Represents a single rule definition within the create rules request.
   *
   * <p>Each rule must have a valid configuration that matches its rule type (BATCH or STREAM).
   *
   * <p>Start time and end time must be in the future, and end time must be after start time.
   */
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @ValidRuleTypeConfiguration
  @ValidTimeRange
  @JsonIgnoreProperties()
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Rule {
    /** The name of the rule. */
    @NotNull(message = "Rule name is required")
    private String name;

    /** A description of the rule's purpose or logic. */
    @NotNull(message = "Rule description is required")
    private String description;

    /** The start time for rule execution (epoch seconds). Must be in the future. */
    @NotNull(message = "Start time is required")
    @ValidFutureEpoch(minDeltaSeconds = 60)
    private Long startTime;

    /** The end time for rule execution (epoch seconds). Must be in the future. */
    @NotNull(message = "End time is required")
    @ValidFutureEpoch
    private Long endTime;

    /** The type of rule (BATCH or STREAM). */
    @NotNull(message = "Rule type is required")
    private RuleType ruleType;

    /** The action to perform when the rule matches (ADD or REMOVE). */
    @NotNull(message = "Rule action is required")
    private RuleAction ruleAction;

    /** The rule configuration containing execution details specific to the rule type. */
    @Valid
    @NotNull(message = "Rule configuration is required")
    private RuleConfiguration<SourceInfo> configuration;
  }
}
