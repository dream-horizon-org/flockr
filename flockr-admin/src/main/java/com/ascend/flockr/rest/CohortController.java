package com.ascend.flockr.rest;

import com.ascend.flockr.io.response.CohortInfoVerbose;
import com.ascend.flockr.io.response.Response;
import com.ascend.flockr.service.web.CohortService;
import com.ascend.flockr.util.ResponseWrapper;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;

public class CohortController {

  private final CohortService cohortService;

  public CohortController(CohortService cohortService) {
    this.cohortService = cohortService;
  }

  @GET
  @Path("/cohorts/{cohortId}")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response<CohortInfoVerbose>> findCohortInfoVerboseById(
      @NotNull @PathParam("cohortId") Long cohortId) {
    return ResponseWrapper.fromMaybe(cohortService.findCohortInfoVerboseById(cohortId), null, 200);
  }
}
