package io.ascend.flockr.admin.domain.audit;

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
