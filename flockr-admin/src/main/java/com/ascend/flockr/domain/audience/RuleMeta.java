package com.ascend.flockr.domain.audience;

import java.time.LocalDateTime;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleMeta {
  private String tenantId;
  private Long ruleId;
  private Long audienceId;
  private String name;
  private String description;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String cronExpression;
  private String ruleAction;
  private String ruleType;
  private String status;
  private String orgName;
  private String createdBy;
  private String updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Boolean resumeUsingLatestArtifact;
  private Boolean resumeFromSavepoint;
}
