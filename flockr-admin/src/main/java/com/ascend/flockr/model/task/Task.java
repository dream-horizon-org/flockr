package com.ascend.flockr.model.task;

import com.ascend.flockr.model.task.constant.RuleAction;
import com.ascend.flockr.model.task.constant.RuleType;
import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import lombok.Data;

@Data
public class Task {

  private Long id;
  private Long cohortId;
  private String name;
  private String description;
  private TaskType type;
  private RuleType ruleType;
  private RuleAction action;
  private Long startTime; /* epoch seconds */
  private Long endTime; /* epoch seconds */
  private String cronExpression; /* unix, UTC */
  private TaskStatus status;
  private String client;
  private Long createdAt; /* epoch seconds */
  private String createdBy;
}
