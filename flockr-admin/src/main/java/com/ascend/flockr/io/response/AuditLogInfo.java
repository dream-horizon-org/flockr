package com.ascend.flockr.io.response;

import com.ascend.flockr.model.task.constant.RuleAction;
import lombok.Data;

@Data
public class AuditLogInfo {
  private Long id;
  private Long cohortId;
  private Long taskId;
  private String action;
  private Object oldValue;
  private Object newValue;
  private String createdBy;
  private Long createdAt;
  private String name;
  private RuleAction ruleAction;
  private String type;
}
