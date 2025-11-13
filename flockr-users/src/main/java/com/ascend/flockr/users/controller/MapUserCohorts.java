package com.ascend.flockr.users.controller;

import com.ascend.flockr.common.exception.errors.DefinedErrors;
import com.ascend.flockr.users.dto.ResponseEntity;
import com.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import com.ascend.flockr.users.service.UserCohortsService;
import com.dream11.rest.util.ExceptionUtil;
import com.google.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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
   * <p>Request body should contain:
   *
   * <ul>
   *   <li>{@code userId} or {@code guestId} - User identifier
   *   <li>{@code cohortKey} - Cohort name
   *   <li>{@code source} - Source identifier
   *   <li>{@code action} - "append" or "remove"
   *   <li>{@code expireAt} - Expiry time (for append action)
   *   <li>{@code projectId} - Project ID (optional)
   * </ul>
   *
   * @param request the mapping request
   * @return CompletionStage resolving to HTTP 200 with success status, or 400 if validation fails
   * @since 1.0
   */
  @POST
  @Path("/map-cohorts")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response> handle(MapUserCohortsRequest request) {
    request.validate();

    if (request.getUserId() == null && request.getGuestId() == null) {
      log.error("userId and guestId provided are null");
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }

    return userCohortsService
        .mapUserCohorts(request)
        .map(ResponseEntity.Success::new)
        .map(res -> Response.ok(res).build())
        .toCompletionStage();
  }
}
