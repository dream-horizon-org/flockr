package com.ascend.flockr.users.controller;

import com.ascend.flockr.common.exception.errors.DefinedErrors;
import com.ascend.flockr.users.dto.ResponseEntity;
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
 * REST controller for retrieving user cohorts.
 *
 * <p>Provides endpoint to get active cohorts for a user identified by userId. This is a
 * multi-tenant endpoint that requires tenantId and projectId for proper data isolation.
 *
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
   * <p>Query parameters:
   *
   * <ul>
   *   <li>{@code userId} - User ID (required, must be positive)
   *   <li>{@code tenantId} - Tenant ID (required, must not be blank)
   *   <li>{@code projectId} - Project ID (required, must be positive)
   * </ul>
   *
   * <p>The set name used for Aerospike operations is generated as "{tenantId}_{projectId}" to
   * ensure multi-tenant isolation.
   *
   * @param userId the user ID from query parameter
   * @param tenantId the tenant ID from query parameter
   * @param projectId the project ID from query parameter
   * @return CompletionStage resolving to HTTP 200 with list of cohort names, or 400 if validation
   *     fails
   * @since 1.0
   */
  @GET
  @Path("/get-cohorts")
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response> handle(
      @QueryParam("userId") Long userId,
      @QueryParam("tenantId") String tenantId,
      @QueryParam("projectId") Long projectId) {

    validate(userId, tenantId, projectId);

    return userCohortsService
        .getCohorts(userId, tenantId, projectId)
        .map(ResponseEntity.Success::new)
        .map(res -> Response.ok(res).build())
        .toCompletionStage();
  }

  /**
   * Validates request parameters.
   *
   * @param userId the user ID to validate
   * @param tenantId the tenant ID to validate
   * @param projectId the project ID to validate
   * @throws IllegalArgumentException if validation fails
   */
  private void validate(Long userId, String tenantId, Long projectId) {
    if (userId == null || userId <= 0) {
      log.error("Invalid userId provided: {}", userId);
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }
    try {
      SetNameUtil.validateTenantAndProject(tenantId, projectId);
    } catch (IllegalArgumentException e) {
      log.error("Invalid tenantId or projectId: tenantId={}, projectId={}", tenantId, projectId);
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }
  }
}
