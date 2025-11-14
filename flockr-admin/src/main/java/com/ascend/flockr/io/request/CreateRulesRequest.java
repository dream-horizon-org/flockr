package com.ascend.flockr.io.request;

import com.ascend.flockr.domain.rule.RuleAction;
import com.ascend.flockr.domain.rule.RuleConfiguration;
import com.ascend.flockr.domain.rule.RuleType;
import com.ascend.flockr.domain.rule.SourceInfoBasic;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateRulesRequest {
  private Long audienceId;
  private List<Rule> rules;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Rule {
    private String name;
    private String description;

    @JsonProperty("start_time")
    private Long startTime;

    @JsonProperty("end_time")
    private Long endTime;

    @JsonProperty("rule_type")
    private RuleType ruleType;

    @JsonProperty("rule_action")
    private RuleAction ruleAction;

    private RuleConfiguration<SourceInfoBasic> configuration;
  }
}
