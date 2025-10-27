package com.ascend.flockr.model.crontrigger;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CronTrigger {
  private Long id;
  private Long taskId;
  private CronTriggerStatus status;
  private String requestId;
  private Long executionId;
  private String cronExpression;
  private Long nextExecutionTime;
  private Long createdAt;
  private Long updatedAt;
}
