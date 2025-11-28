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
 * multi-tenant endpoint that requires x-project-key header for proper data isolation.
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
   * <p>Headers:
   *
   * <ul>
   *   <li>{@code userId} - User ID (required, must be positive)
   *   <li>{@code x-project-key} - Combined tenant and project identifier in format
   *       "tenantId_projectId" (required)
   * </ul>
   *
   * <p>The set name used for Aerospike operations is generated from x-project-key to ensure
   * multi-tenant isolation.
   *
   * @param userIdHeader the user ID from userId header
   * @param projectKey the combined tenant and project identifier from x-project-key header
   * @return CompletionStage resolving to HTTP 200 with list of cohort names, or 400 if validation
   *     fails
   * @since 1.0
   */
  @GET
  @Path("/get-cohorts")
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response> handle(
      @HeaderParam("userId") String userIdHeader, @HeaderParam("x-project-key") String projectKey) {

    // Validate userId header is present
    if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
      log.error("Missing userId header");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_USER_ID_HEADER);
    }

    // Validate x-project-key header is present
    if (projectKey == null || projectKey.trim().isEmpty()) {
      log.error("Missing x-project-key header");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_PROJECT_KEY_HEADER);
    }

    // Parse user ID
    Long userId;
    try {
      userId = Long.parseLong(userIdHeader.trim());
      if (userId <= 0) {
        log.error("Invalid userId provided: {}", userIdHeader);
        throw ExceptionUtil.getException(DefinedErrors.INVALID_USER_ID, userIdHeader);
      }
    } catch (IllegalArgumentException | NullPointerException e) {
      log.error("Invalid userId format: {}", userIdHeader);
      throw ExceptionUtil.getException(DefinedErrors.INVALID_USER_ID, userIdHeader);
    }

    // Parse x-project-key (format: tenantId_projectId)
    String[] projectKeyParts = projectKey.split("_", 2);
    if (projectKeyParts.length != 2) {
      log.error("Invalid x-project-key format: {}", projectKey);
      throw ExceptionUtil.getException(DefinedErrors.INVALID_PROJECT_KEY_FORMAT, projectKey);
    }

    String tenantId = projectKeyParts[0].trim();
    String projectId = projectKeyParts[1].trim();

    // Validate tenantId is not empty
    if (tenantId.isEmpty()) {
      log.error("Empty tenantId in x-project-key: {}", projectKey);
      throw ExceptionUtil.getException(DefinedErrors.MISSING_TENANT_ID);
    }

    // Validate projectId is not empty
    if (projectId.isEmpty()) {
      log.error("Empty projectId in x-project-key: {}", projectKey);
      throw ExceptionUtil.getException(DefinedErrors.MISSING_PROJECT_ID);
    }

    // Validate tenantId and projectId (includes UUID validation for tenantId)
    try {
      SetNameUtil.validateTenantAndProject(tenantId, projectId);
    } catch (IllegalArgumentException e) {
      log.error(
          "Invalid tenantId or projectId: tenantId={}, projectId={}, error={}",
          tenantId,
          projectId,
          e.getMessage());
      // Check if it's a UUID validation error
      if (e.getMessage().contains("UUID")) {
        throw ExceptionUtil.getException(DefinedErrors.INVALID_TENANT_ID_FORMAT, tenantId);
      } else {
        throw ExceptionUtil.getException(DefinedErrors.INVALID_PROJECT_KEY_FORMAT, projectKey);
      }
    }

    return userCohortsService
        .getCohorts(userId, tenantId, projectId)
        .map(ResponseEntity.Success::new)
        .map(res -> Response.ok(res).build())
        .toCompletionStage();
  }
}
