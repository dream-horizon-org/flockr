package io.ascend.flockr.users.controller;

import com.google.inject.Inject;
import io.ascend.flockr.users.dto.ResponseEntity;
import io.ascend.flockr.users.service.UserCohortsService;
import io.ascend.flockr.users.util.ErrorHandler;
import io.ascend.flockr.users.validator.HeaderValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for retrieving user cohorts.
 *
 * <p>Provides endpoint to get active cohorts for a user identified by userId. This is a
 * multi-tenant endpoint that requires x-project-key header for proper data isolation.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
@Path("/flockr/users/")
@Tag(name = "User Cohorts", description = "Operations for retrieving user cohort memberships")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class GetUserCohorts {
  private final UserCohortsService userCohortsService;

  /**
   * Retrieves active cohorts for a user.
   *
   * @param userIdHeader the user ID from userId header
   * @param projectKey the combined tenant and project identifier from x-project-key header
   * @return CompletionStage resolving to HTTP 200 with list of cohort names
   */
  @GET
  @Path("/get-cohorts")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get user cohorts",
      description =
          "Retrieves a list of active cohort names that the specified user belongs to. "
              + "Only returns cohorts that have not expired.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved user cohorts",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                array = @ArraySchema(schema = @Schema(implementation = String.class)))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request - missing or invalid userId/x-project-key header",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ResponseEntity.Failure.class)))
  })
  public CompletionStage<ResponseEntity.Success<List<String>>> handle(
      @NotNull(message = "userId header is required")
      @Parameter(required = true, example = "12345") @HeaderParam("userId") String userIdHeader,
      @NotNull(message = "x-project-key header is required")
      @Parameter(
              description = "Project key used as Aerospike set name for multi-tenant isolation",
              required = true,
              example = "tenant1_project1")
          @HeaderParam("x-project-key")
          String projectKey) {

    // Validate headers
    HeaderValidator.validateProjectKeyHeader(projectKey);

    return ErrorHandler.handleAsync(
        userCohortsService.getCohorts(userIdHeader, projectKey), "getUserCohorts");
  }
}
