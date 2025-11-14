package com.ascend.flockr.rest;

import com.ascend.flockr.io.ResponseEntity;
import com.ascend.flockr.io.request.CreateAudienceRequest;
import com.ascend.flockr.io.request.CreateRulesRequest;
import com.ascend.flockr.io.response.AudienceDetailsResponse;
import com.ascend.flockr.io.response.RuleDetailsResponse;
import com.ascend.flockr.service.AudienceService;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;

@Path("/")
@Tag(name = "Audiences", description = "Audience management APIs")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AudienceController {

  private final AudienceService audienceService;

  @POST
  @Path("/v1/audiences")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create a new audience",
      description = "Creates a new audience and returns the audience ID")
  @ApiResponse(
      responseCode = "200",
      description = "Audience created successfully",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid/missing body params",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
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
  @Operation(
      summary = "Get audience details",
      description = "Retrieves detailed information about a specific audience")
  @ApiResponse(
      responseCode = "200",
      description = "Successful Response",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(responseCode = "304", description = "Successful Not Modified Response")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid/missing body params / header",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<AudienceDetailsResponse>> getAudienceDetails(
      @Parameter(description = "ID of the audience to retrieve", required = true)
          @PathParam("audienceId")
          Long audienceId) {
    return audienceService
        .getAudienceDetails(audienceId)
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }

  @POST
  @Path("/v1/audiences/{audienceId}/rules")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create rules for an audience",
      description = "Creates rules for a specific audience")
  @ApiResponse(
      responseCode = "200",
      description = "Rules created successfully",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid/missing body params",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<Boolean>> createRules(
      @Parameter(description = "ID of the audience", required = true) @PathParam("audienceId")
          Long audienceId,
      CreateRulesRequest requestBody) {

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
  @Operation(
      summary = "Get rule details",
      description = "Retrieves detailed information about a specific rule within an audience")
  @ApiResponse(
      responseCode = "200",
      description = "Successful Response",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid/missing parameters",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Rule not found",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<RuleDetailsResponse>> getRuleDetails(
      @Parameter(description = "ID of the audience", required = true) @PathParam("audienceId")
          Long audienceId,
      @Parameter(description = "ID of the rule to retrieve", required = true) @PathParam("ruleId")
          Long ruleId) {
    return audienceService
        .getRuleDetails(audienceId, ruleId)
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }
}
