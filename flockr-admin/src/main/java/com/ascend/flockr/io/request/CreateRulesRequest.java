package com.ascend.flockr.io.request;

import com.ascend.flockr.domain.rule.*;
import com.ascend.flockr.validation.ValidRuleTypeConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties()
public class CreateRulesRequest {
  private Long audienceId;
  @Valid @NotEmpty private List<Rule> rules;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @ValidRuleTypeConfiguration
  @JsonIgnoreProperties()
  public static class Rule {
    @NotNull private String name;
    @NotNull private String description;

    @NotNull
    @JsonProperty("start_time")
    private Long startTime;

    @NotNull
    @JsonProperty("end_time")
    private Long endTime;

    @NotNull
    @JsonProperty("rule_type")
    private RuleType ruleType;

    @NotNull
    @JsonProperty("rule_action")
    private RuleAction ruleAction;

    @Valid @NotNull private RuleConfiguration<SourceInfo> configuration;
  }
}
