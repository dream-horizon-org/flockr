package com.ascend.flockr.dao.taskdefinition;

import com.ascend.flockr.model.task.constant.RuleAction;
import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import lombok.Data;

@Data
public class TaskWithJobInfo {

  private Long id;
  private String name;
  private TaskType type;
  private RuleAction action;
  private String cronExpression;
  private TaskStatus taskStatus;
  private Long taskUpdatedAt;
  private String jobId;
  private Long jobCreatedAt;
  private Long jobUpdatedAt;
  private String jobStatus;
}
