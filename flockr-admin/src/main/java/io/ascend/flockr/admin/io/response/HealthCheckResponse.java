package io.ascend.flockr.admin.io.response;

public record HealthCheckResponse(Boolean isMySQLReaderUp, Boolean isUnderMaintenance) {}
