package com.ascend.flockr.model.AuditLog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogValue {
  private Object oldValue;
  private Object newValue;
}
