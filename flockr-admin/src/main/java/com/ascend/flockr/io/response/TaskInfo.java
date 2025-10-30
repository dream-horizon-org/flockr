package com.ascend.flockr.io.response;

import com.ascend.flockr.model.task.constant.RuleAction;
import com.ascend.flockr.model.task.constant.RuleType;
import com.ascend.flockr.model.task.constant.TaskStatus;
import lombok.Data;

@Data
public class TaskInfo {

  private Long id;
  private Long cohortId;
  private String name;
  private String description;
  private String type;
  private RuleType ruleType;
  private RuleAction action;
  private Long startDate;
  private Long endDate;
  private String cronExpression;
  private TaskStatus status;
  private String client;
  private String createdAt;
  private String createdBy;
}
