package io.ascend.flockr.users.controller;

import com.google.inject.Inject;
import io.ascend.flockr.users.service.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Path("/healthcheck")
@Tag(name = "Health Check", description = "Application health and readiness endpoints")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class HealthCheck {

  private final HealthCheckService healthCheckService;

  /**
   * Performs a health check and returns the status.
   *
   * @return CompletionStage resolving to a JsonObject with health status information
   */
  @GET
  @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Health check",
      description =
          "Returns the health status of the application and its dependencies including Aerospike connection status.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Application is healthy",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = Object.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"status\": \"UP\", \"aerospike\": \"connected\", \"timestamp\": 1701619200000}"))),
    @ApiResponse(responseCode = "503", description = "Application is unhealthy or degraded")
  })
  public CompletionStage<JsonObject> handle() {
    return healthCheckService.healthCheck().toCompletionStage();
  }
}
