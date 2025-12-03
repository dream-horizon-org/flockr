package com.ascend.flockr.users.controller;

import com.ascend.flockr.users.service.HealthCheckService;
import com.google.inject.Inject;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for health check operations.
 *
 * <p>Provides an endpoint to check the health status of the application and its dependencies.
 *
 * @since 1.0
 */
@Path("/healthcheck")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class HealthCheck {

  private final HealthCheckService healthCheckService;

  /**
   * Performs a health check and returns the status.
   *
   * <p>Endpoint: GET /healthcheck
   *
   * <p>Returns a JSON object containing the health status of the application and its dependencies
   * (e.g., Aerospike connection status).
   *
   * @return CompletionStage resolving to a JsonObject with health status information
   */
  @GET
  @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<JsonObject> handle() {
    return healthCheckService.healthCheck().toCompletionStage();
  }
}
