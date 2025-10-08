package com.ascend.flockr.dto.response;

public record HealthCheckResponse(
        Boolean isMySQLReaderUp,
                Boolean isAerospikeup,
            Boolean isUnderMaintenance
) {}
