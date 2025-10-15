package com.ascend.flockr.users.controller;

import com.ascend.flockr.users.service.UserCohortsService;
import com.dream11.rest.util.ExceptionUtil;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.CompletionStage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/flockr")
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
      @QueryParam("source") String source) {

    validate(userId, guestId);

    return userCohortsService
        .getCohorts(userId, guestId, source)
        .map(ResponseEntity.Success::new)
        .map(res -> Response.ok(res).build())
        .toCompletionStage();
  }

  private void validate(Long userId, String guestId) {
    if (userId == null && guestId == null) {
      log.error("userId and guestId provided are null ");
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }
    if (userId != null && userId <= 0) {
      log.error("Invalid request parameters provided: userId and guestId");
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }
  }
}
