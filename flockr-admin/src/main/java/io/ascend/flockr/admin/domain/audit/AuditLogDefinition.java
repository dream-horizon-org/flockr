package io.ascend.flockr.admin.domain.audit;

import io.ascend.flockr.admin.domain.rule.RuleAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogDefinition {
    private Long id;
    private Long cohortId;
    private Long taskId;
    private AuditLogAction action;
    private AuditLogValue value;
    private String createdBy;
    private Long createdAt;
    private String name;
    private RuleAction ruleAction;
    private TaskType type;
}


