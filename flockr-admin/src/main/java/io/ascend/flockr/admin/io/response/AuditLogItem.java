package io.ascend.flockr.admin.io.response;

import java.time.Instant;

public record AuditLogItem(String action, String performedBy, Instant performedAt, String details) {}

