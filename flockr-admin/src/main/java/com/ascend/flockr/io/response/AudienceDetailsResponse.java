package com.ascend.flockr.io.response;

import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.rule.RuleConfiguration;
import com.ascend.flockr.domain.rule.SourceInfoEnriched;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudienceDetailsResponse {
  private Long audienceId;
  private String tenantId;
  private String projectId;
  private String name;
  private String description;
  private String type;
  private JsonNode customAudienceConfig;
  private Boolean verified;
  private Long userCount;
  private Integer rulesCount;
  private Long expireDate;
  private Long lastAudienceUpdatedAt;
  private Long createdAt;
  private Long updatedAt;
  private String createdBy;

  // Associated sinks with metadata
  private List<DataSinkDetails> sinks;

  // Associated rules with enriched configuration
  private List<RuleDetails> rules;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RuleDetails {
    private Long ruleId;
    private String name;
    private String description;
    private Long startTime;
    private Long endTime;
    private String ruleAction;
    private String ruleType;
    private String status;
    private RuleConfiguration<SourceInfoEnriched>
        configuration; // Polymorphic: BatchConfig or StreamConfig with metadata
    private String createdBy;
    private Long createdAt;
    private Long updatedAt;
  }
}
