package io.ascend.flockr.admin.domain.audit;

/**
 * Entity categories that can be audited in the system.
 *
 * <p>This enum is intentionally simple and maps directly to the persisted string values in the
 * audit_logs table (entity_type, related_entity_type).
 */
public enum AuditEntityType {
    AUDIENCE,
    RULE,
    AUDIENCE_OWNER
}

