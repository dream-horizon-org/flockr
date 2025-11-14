package com.ascend.flockr.rest;

import com.ascend.flockr.io.ResponseEntity;
import com.ascend.flockr.io.request.CreateAudienceRequest;
import com.ascend.flockr.io.request.CreateRulesRequest;
import com.ascend.flockr.io.response.AudienceDetailsResponse;
import com.ascend.flockr.io.response.RuleDetailsResponse;
import com.ascend.flockr.service.AudienceService;
import com.google.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AudienceController {

  private final AudienceService audienceService;

  @POST
  @Path("/v1/audiences")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<ResponseEntity.Success<Long>> createAudience(
      CreateAudienceRequest requestBody) {
    return audienceService
        .createAudience(requestBody)
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }

  @GET
  @Path("/v1/audiences/{audienceId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<ResponseEntity.Success<AudienceDetailsResponse>> getAudienceDetails(
      @PathParam("audienceId") Long audienceId) {
    return audienceService
        .getAudienceDetails(audienceId)
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }

  @POST
  @Path("/v1/audiences/{audienceId}/rules")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<ResponseEntity.Success<Boolean>> createRules(
      @PathParam("audienceId") Long audienceId, CreateRulesRequest requestBody) {

    requestBody.setAudienceId(audienceId);
    return audienceService
        .createRules(requestBody)
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }

  @GET
  @Path("/v1/audiences/{audienceId}/rules/{ruleId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<ResponseEntity.Success<RuleDetailsResponse>> getRuleDetails(
      @PathParam("audienceId") Long audienceId, @PathParam("ruleId") Long ruleId) {
    return audienceService
        .getRuleDetails(audienceId, ruleId)
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }
}
