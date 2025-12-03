package io.ascend.flockr.users.controller;

import com.google.inject.Inject;
import io.ascend.flockr.users.dto.ResponseEntity;
import io.ascend.flockr.users.service.UserCohortsService;
import io.ascend.flockr.users.validator.HeaderValidator;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class GetUserCohorts {
  private final UserCohortsService userCohortsService;

  /**
   * Retrieves active cohorts for a user.
   *
   * <p>Endpoint: GET /flockr/users/get-cohorts
   *
   * <p>Headers:
   *
   * <ul>
   *   <li>{@code userId} - User ID (required, must be positive)
   *   <li>{@code x-project-key} - Project key used directly as Aerospike set name (required)
   * </ul>
   *
   * <p>The x-project-key is used directly as the Aerospike set name for multi-tenant isolation.
   *
   * @param userIdHeader the user ID from userId header
   * @param projectKey the combined tenant and project identifier from x-project-key header
   * @return CompletionStage resolving to HTTP 200 with list of cohort names, or 400 if validation
   *     fails
   * @author Sudhanshu Rai
   * @since 1.0
   */
  @GET
  @Path("/get-cohorts")
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response> handle(
      @HeaderParam("userId") String userIdHeader, @HeaderParam("x-project-key") String projectKey) {

    // Validate headers
    HeaderValidator.validateProjectKeyHeader(projectKey);
    Long userId = HeaderValidator.validateAndParseUserId(userIdHeader);

    return userCohortsService
        .getCohorts(userId, projectKey)
        .map(ResponseEntity.Success::new)
        .map(res -> Response.ok(res).build())
        .toCompletionStage();
  }
}
