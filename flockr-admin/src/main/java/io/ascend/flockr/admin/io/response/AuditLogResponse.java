package io.ascend.flockr.admin.io.response;


import java.util.List;

public record AuditLogResponse(String date, List<AuditLogItem> items) {}


