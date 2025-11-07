package com.ascend.flockr.domain.task;

import lombok.Data;

@Data
public class TaskBaseInfo {
  // Core identification fields
  private Long id;
  private Long cohortId;
  private String name;
  private String description;

  // Scheduling fields
  private Long startTime; /* epoch seconds */
  private Long endTime; /* epoch seconds */
  private String cronExpression; /* unix, UTC */

  // client information
  private String client;

  // Audit fields
  private Long createdAt; /* epoch seconds */
  private String createdBy;
}
