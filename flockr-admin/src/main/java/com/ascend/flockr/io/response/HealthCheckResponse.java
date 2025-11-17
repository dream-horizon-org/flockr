package com.ascend.flockr.io.response;

public record HealthCheckResponse(Boolean isMySQLReaderUp, Boolean isUnderMaintenance) {}
