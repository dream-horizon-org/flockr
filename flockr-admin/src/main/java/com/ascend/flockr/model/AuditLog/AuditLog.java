package com.ascend.flockr.model.AuditLog;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLog {

  private Long id;
  private Long cohortId;
  private Long taskId;
  private AuditLogAction action;
  private AuditLogValue value;
  private String createdBy;
}
