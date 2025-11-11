package com.ascend.flockr.rest;

import com.ascend.flockr.io.ResponseEntity;
import com.ascend.flockr.io.request.CreateAudienceRequest;
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
  @Path("/v1/audience")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<ResponseEntity.Success<Long>> createAudience(
      CreateAudienceRequest requestBody) {
    return audienceService
        .createAudience(requestBody)
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }
}
