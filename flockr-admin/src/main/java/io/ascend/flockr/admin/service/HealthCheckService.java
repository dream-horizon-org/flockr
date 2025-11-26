package io.ascend.flockr.admin.service;

import io.ascend.flockr.admin.io.response.HealthCheckResponse;
import io.reactivex.rxjava3.core.Single;

/**
 * Service interface for health check operations.
 *
 * <p>This service provides methods to check the health status of the application and its
 * dependencies.
 *
 * @author Flockr Team
 * @since 1.0
 */
public interface HealthCheckService {
  /**
   * Performs a health check and returns the application health status.
   *
   * @return a Single emitting the health check response
   */
  Single<HealthCheckResponse> healthCheck();
}
