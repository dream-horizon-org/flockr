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

@Slf4j
@Path("/flockr")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class GetUserCohorts {
  private final UserCohortsService userCohortsService;

  @GET
  @Path("/get-user-cohorts")
  @Consumes(MediaType.APPLICATION_JSON)
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
