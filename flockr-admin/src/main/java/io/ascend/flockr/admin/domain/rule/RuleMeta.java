package io.ascend.flockr.admin.domain.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleMeta<T extends SourceInfo> {
  private String tenantId;
  private String projectId;
  private Long ruleId;
  private Long audienceId;
  private String name;
  private String description;
  private Long startTime;
  private Long endTime;
  private RuleAction ruleAction;
  private RuleType ruleType;
  private RuleStatus status;
  private RuleConfiguration<T> configuration;
  private String createdBy;
  private Long createdAt;
  private Long updatedAt;
}
