package com.ascend.flockr.domain.task;

import com.ascend.flockr.domain.rule.enums.RuleAction;
import com.ascend.flockr.domain.rule.enums.RuleType;
import com.ascend.flockr.domain.task.enums.TaskStatus;
import com.ascend.flockr.domain.task.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing a Task entity.
 *
 * <p>This class contains all task-related fields including core task information, rule
 * configuration, sequence data, and metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

  // Core identification fields
  private TaskBaseInfo taskBaseInfo;

  // Task type and rule configuration
  private TaskType type;
  private RuleType ruleType;
  private RuleAction action;

  // Status
  private TaskStatus status;
}
