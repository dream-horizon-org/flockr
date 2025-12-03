package io.ascend.flockr.users.service;

import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;

/**
 * Service interface for health check operations.
 *
 * <p>This interface is currently empty and reserved for future health check functionality.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
public interface HealthCheckService {
  /**
   * Performs a health check of the application and its dependencies.
   *
   * <p>This method checks the connectivity status of critical dependencies (e.g., Aerospike) and
   * returns a JSON object containing the health status.
   *
   * @return a Single emitting a JsonObject containing health status information
   */
  Single<JsonObject> healthCheck();
}
