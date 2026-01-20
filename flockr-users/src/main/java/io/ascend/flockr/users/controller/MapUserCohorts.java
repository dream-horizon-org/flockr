package io.ascend.flockr.users.controller;

import com.google.inject.Inject;
import io.ascend.flockr.users.dto.BulkOperationResult;
import io.ascend.flockr.users.dto.ResponseEntity;
import io.ascend.flockr.users.dto.request.BatchMapUserCohortsRequest;
import io.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import io.ascend.flockr.users.service.UserCohortsService;
import io.ascend.flockr.users.util.ErrorHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for mapping users to cohorts.
 *
 * <p>Provides endpoint to assign or remove users from cohorts.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
@Path("/flockr/users")
@Tag(
    name = "User Cohort Mapping",
    description = "Operations for assigning or removing users from cohorts")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class MapUserCohorts {

  private final UserCohortsService userCohortsService;

  /**
   * Maps a user to a cohort (assigns or removes).
   *
   * @param userIdHeader the user ID from userId header
   * @param projectKey the combined tenant and project identifier from x-project-key header
   * @param request the mapping request (cohort_key, action, expire_at)
   * @return CompletionStage resolving to HTTP 200 with success status
   */
  @POST
  @Path("/map-cohorts")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Map user to cohort",
      description =
          "Assigns or removes a user from a cohort. Use action 'append' to add a user with an expiry time, "
              + "or 'remove' to remove the user from the cohort.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "User successfully mapped to cohort",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ResponseEntity.Success.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request - missing or invalid parameters",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ResponseEntity.Failure.class)))
  })
  public CompletionStage<ResponseEntity.Success<Boolean>> handle(
      @NotBlank(message = "userId header is required")
          @Parameter(description = "User ID (must be positive)", required = true, example = "12345")
          @HeaderParam("userId")
          String userIdHeader,
      @NotBlank(message = "x-project-key header is required")
          @Parameter(
              description = "Project key used as Aerospike set name for multi-tenant isolation",
              required = true,
              example = "tenant1_project1")
          @HeaderParam("x-project-key")
          String projectKey,
      @RequestBody(
              description = "Cohort mapping request",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      schema = @Schema(implementation = MapUserCohortsRequest.class)))
          @Valid
          MapUserCohortsRequest request) {

    return ErrorHandler.handleAsync(
        userCohortsService.mapUserCohorts(userIdHeader, projectKey, request), "mapUserCohorts");
  }

  /**
   * Batch maps multiple users to cohorts (assigns or removes).
   *
   * @param projectKey the project key from x-project-key header
   * @param requests the list of mapping requests
   * @return CompletionStage resolving to HTTP 200 with bulk operation result
   */
  @POST
  @Path("/map-cohorts/batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Batch map users to cohorts",
      description =
          "Batch operation to assign or remove multiple users from cohorts in a single request. "
              + "Each item in the request array specifies a user, cohort, action, and expiry time.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Batch operation completed",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = BulkOperationResult.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request - missing or invalid parameters",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ResponseEntity.Failure.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error during batch processing",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ResponseEntity.Failure.class)))
  })
  public CompletionStage<ResponseEntity.Success<BulkOperationResult>> handleBatch(
      @NotBlank(message = "x-project-key header is required")
          @Parameter(
              description = "Project key used as Aerospike set name for multi-tenant isolation",
              required = true,
              example = "tenant1_project1")
          @HeaderParam("x-project-key")
          String projectKey,
      @RequestBody(
              description = "List of cohort mapping requests",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      schema = @Schema(implementation = BatchMapUserCohortsRequest[].class)))
          @Valid
          List<BatchMapUserCohortsRequest> requests) {

    return ErrorHandler.handleAsync(
        userCohortsService.batchMapUserCohorts(projectKey, requests), "batchMapUserCohorts");
  }
}
