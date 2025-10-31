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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionStage;

@Slf4j
@Path("/user-cohort")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class MapUserCohorts {

    private final UserCohortsService userCohortsService;

    @POST
    @Path("/map")
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
