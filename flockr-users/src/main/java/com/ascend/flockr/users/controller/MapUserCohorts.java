package com.ascend.flockr.users.controller;

import com.ascend.flockr.users.dto.ResponseEntity;
import com.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import com.ascend.flockr.users.service.UserCohortsService;
import com.ascend.flockr.users.validator.HeaderValidator;
import com.ascend.flockr.users.validator.MapUserCohortsRequestValidator;
import com.google.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for mapping users to cohorts.
 *
 * <p>Provides endpoint to assign or remove users from cohorts.
 *
 * @since 1.0
 */
@Slf4j
@Path("/flockr/users")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class MapUserCohorts {

  private final UserCohortsService userCohortsService;

  /**
   * Maps a user to a cohort (assigns or removes).
   *
   * <p>Endpoint: POST /flockr/users/map-cohorts
   *
   * <p>Headers:
   *
   * <ul>
   *   <li>{@code userId} - User ID (required, must be positive)
   *   <li>{@code x-project-key} - Project key used directly as Aerospike set name (required)
   * </ul>
   *
   * <p>Request body should contain:
   *
   * <ul>
   *   <li>{@code cohort_key} - Cohort name (snake_case for API)
   *   <li>{@code action} - "append" or "remove"
   *   <li>{@code expire_at} - Expiry time in format "yyyy-MM-dd HH:mm:ss" (for append action)
   * </ul>
   *
   * <p>The x-project-key is used directly as the Aerospike set name for multi-tenant isolation.
   *
   * @param userIdHeader the user ID from userId header
   * @param projectKey the combined tenant and project identifier from x-project-key header
   * @param request the mapping request (cohort_key, action, expire_at)
   * @return CompletionStage resolving to HTTP 200 with success status, or 400 if validation fails
   * @since 1.0
   */
  @POST
  @Path("/map-cohorts")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response> handle(
      @HeaderParam("userId") String userIdHeader,
      @HeaderParam("x-project-key") String projectKey,
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
}
