package io.ascend.flockr.admin.io.response;

/**
 * Response containing the health status of the application.
 *
 * <p>This response is used by health check endpoints to report the status of critical system
 * dependencies and operational modes.
 *
 * @param isPostgresReaderUp true if the PostgreSQL reader connection is healthy
 * @param isUnderMaintenance true if the application is in maintenance mode
 * @author Prithu Sharma
 * @since 1.0
 */
public record HealthCheckResponse(Boolean isPostgresReaderUp, Boolean isUnderMaintenance) {}
