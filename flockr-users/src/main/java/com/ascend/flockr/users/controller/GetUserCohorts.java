package com.ascend.flockr.users.controller;

import com.ascend.flockr.common.exception.errors.DefinedErrors;
import com.ascend.flockr.users.dto.ResponseEntity;
import com.ascend.flockr.users.service.UserCohortsService;
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
 * <p>Provides endpoint to get active cohorts for a user identified by either userId or guestId.
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
   *   <li>{@code userId} - User ID (optional if guestId provided)
   *   <li>{@code guestId} - Guest ID (optional if userId provided)
   *   <li>{@code projectId} - Project ID (required, must be positive)
   * </ul>
   *
   * @param userId the user ID from query parameter
   * @param guestId the guest ID from query parameter
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
      @QueryParam("guestId") String guestId,
      //            @AcceptedValues(values = {Constants.SOURCE_DREAM11, Constants.SOURCE_FANCODE})
      //      @QueryParam("source") String source)
      @QueryParam("projectId") Long projectId) {

    validate(userId, guestId, projectId);

    return userCohortsService
        .getCohorts(userId, guestId, projectId)
        .map(ResponseEntity.Success::new)
        .map(res -> Response.ok(res).build())
        .toCompletionStage();
  }

  /**
   * Validates request parameters.
   *
   * @param userId the user ID to validate
   * @param guestId the guest ID to validate
   * @param projectId the project ID to validate
   * @throws IllegalArgumentException if validation fails
   */
  private void validate(Long userId, String guestId, Long projectId) {
    if (projectId == null || projectId <= 0) {
      log.error("Invalid projectId provided: {}", projectId);
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }
    if (userId == null && guestId == null) {
      log.error("userId and guestId provided are null ");
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }
    if (userId != null && userId <= 0) {
      log.error("Invalid request parameters provided: userId {}", userId);
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }
  }
}
