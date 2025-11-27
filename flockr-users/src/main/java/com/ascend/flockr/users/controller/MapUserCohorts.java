package com.ascend.flockr.users.controller;

import com.ascend.flockr.common.exception.errors.DefinedErrors;
import com.ascend.flockr.users.dto.ResponseEntity;
import com.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import com.ascend.flockr.users.service.UserCohortsService;
import com.ascend.flockr.users.util.SetNameUtil;
import com.dream11.rest.util.ExceptionUtil;
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
   *   <li>{@code x-project-key} - Combined tenant and project identifier in format "tenantId_projectId" (required)
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
   * <p>The set name used for Aerospike operations is generated from x-project-key to
   * ensure multi-tenant isolation.
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

    // Parse user ID
    Long userId;
    try {
      userId = Long.parseLong(userIdHeader);
      if (userId <= 0) {
        throw new IllegalArgumentException("userId must be positive");
      }
    } catch (IllegalArgumentException | NullPointerException e) {
      log.error("Invalid userId provided: {}", userIdHeader);
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }

    // Parse x-project-key (format: tenantId_projectId)
    String[] projectKeyParts = projectKey != null ? projectKey.split("_", 2) : new String[0];
    if (projectKeyParts.length != 2) {
      log.error("Invalid x-project-key format: {}", projectKey);
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }

    String tenantId = projectKeyParts[0].trim();
    String projectId = projectKeyParts[1].trim();
    
    // Validate tenantId and projectId
    SetNameUtil.validateTenantAndProject(tenantId, projectId);

    // Validate request body
    request.validate();

    return userCohortsService
        .mapUserCohorts(userId, tenantId, projectId, request)
        .map(ResponseEntity.Success::new)
        .map(res -> Response.ok(res).build())
        .toCompletionStage();
  }
}
