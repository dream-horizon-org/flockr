package com.ascend.flockr.rest;

import com.ascend.flockr.io.ResponseEntity;
import com.ascend.flockr.io.response.HealthCheckResponse;
import com.ascend.flockr.service.HealthCheckService;
import com.ascend.flockr.util.ErrorHandler;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;

/**
 * REST endpoint for health check monitoring.
 *
 * <p>This endpoint is hidden from Swagger documentation and is typically used by load balancers
 * and monitoring systems to verify the application is running and healthy.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Path("/healthcheck")
@Hidden
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class HealthCheck {

  private final HealthCheckService healthCheckService;

  /**
   * Performs a health check and returns the application health status.
   *
   * @return a CompletionStage that completes with the health check response
   */
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<ResponseEntity.Success<HealthCheckResponse>> healthCheckHandle() {
    return ErrorHandler.handleAsync(healthCheckService.healthCheck(), "healthcheck");
  }
}
