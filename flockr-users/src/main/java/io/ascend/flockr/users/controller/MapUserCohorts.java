package io.ascend.flockr.users.controller;

import com.google.inject.Inject;
import io.ascend.flockr.users.dto.BulkOperationResult;
import io.ascend.flockr.users.dto.ResponseEntity;
import io.ascend.flockr.users.dto.request.BatchMapUserCohortsRequest;
import io.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import io.ascend.flockr.users.service.UserCohortsService;
import io.ascend.flockr.users.validator.BatchMapUserCohortsRequestValidator;
import io.ascend.flockr.users.validator.HeaderValidator;
import io.ascend.flockr.users.validator.MapUserCohortsRequestValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
  public CompletionStage<Response> handle(
      @Parameter(description = "User ID (must be positive)", required = true, example = "12345")
          @HeaderParam("userId")
          String userIdHeader,
      @Parameter(
              description = "Project key used as Aerospike set name for multi-tenant isolation",
              required = true,
              example = "tenant1_project1")
          @HeaderParam("x-project-key")
          String projectKey,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Cohort mapping request",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      schema = @Schema(implementation = MapUserCohortsRequest.class)))
          MapUserCohortsRequest request) {

    // Validate headers
    HeaderValidator.validateProjectKeyHeader(projectKey);
    Long userId = HeaderValidator.validateAndParseUserId(userIdHeader);

    // Validate request body
    MapUserCohortsRequestValidator.validate(request);

    return userCohortsService
        .mapUserCohorts(userId, projectKey, request)
        .map(ResponseEntity.Success::new)
        .map(res -> Response.ok(res).build())
        .toCompletionStage();
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
  public CompletionStage<Response> handleBatch(
      @Parameter(
              description = "Project key used as Aerospike set name for multi-tenant isolation",
              required = true,
              example = "tenant1_project1")
          @HeaderParam("x-project-key")
          String projectKey,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "List of cohort mapping requests",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      schema = @Schema(implementation = BatchMapUserCohortsRequest[].class)))
          List<BatchMapUserCohortsRequest> requests) {

    HeaderValidator.validateProjectKeyHeader(projectKey);
    BatchMapUserCohortsRequestValidator.validate(requests);

    return userCohortsService
        .batchMapUserCohorts(projectKey, requests)
        .map(ResponseEntity.Success<BulkOperationResult>::new)
        .map(Response::ok)
        .map(Response.ResponseBuilder::build)
        .onErrorReturn(error -> buildErrorResponse(error))
        .toCompletionStage();
  }

  /** Builds an error response for batch mapping failures. */
  private Response buildErrorResponse(Throwable error) {
    log.error("Batch cohort mapping failed", error);
    ResponseEntity.Failure failure =
        new ResponseEntity.Failure("BATCH_MAPPING_FAILED", error.getMessage(), null);
    return Response.serverError().entity(failure).build();
  }
}
