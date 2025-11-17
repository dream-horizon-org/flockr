package com.ascend.flockr.rest;

import com.ascend.flockr.io.ResponseEntity;
import com.ascend.flockr.io.request.CreateAudienceRequest;
import com.ascend.flockr.io.request.CreateRulesRequest;
import com.ascend.flockr.io.response.AudienceDetailsResponse;
import com.ascend.flockr.io.response.AudienceMetaResponse;
import com.ascend.flockr.io.response.PaginatedResponse;
import com.ascend.flockr.io.response.RuleDetailsResponse;
import com.ascend.flockr.service.AudienceService;
import com.ascend.flockr.util.ErrorHandler;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing audiences and their associated rules.
 *
 * <p>This controller provides endpoints for:
 * <ul>
 *   <li>Creating and retrieving audiences</li>
 *   <li>Creating and retrieving rules for audiences</li>
 *   <li>Listing audiences with filtering and pagination</li>
 * </ul>
 *
 * <p>All endpoints require tenant and project identifiers via headers (X-Tenant-Id, X-Project-Id).
 *
 * @author
 * @since 1.0
 */
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
      @Parameter(description = "Tenant identifier", required = true) @HeaderParam("X-Tenant-Id")
          String tenantId,
      @Parameter(description = "Project identifier", required = true) @HeaderParam("X-Project-Id")
          String projectId,
      CreateAudienceRequest requestBody) {
    return ErrorHandler.handleAsync(
        audienceService.createAudience(tenantId, projectId, requestBody), "createAudience");
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
      @Parameter(description = "Tenant identifier", required = true) @HeaderParam("X-Tenant-Id")
          String tenantId,
      @Parameter(description = "Project identifier", required = true) @HeaderParam("X-Project-Id")
          String projectId,
      @Parameter(description = "ID of the audience to retrieve", required = true)
          @PathParam("audienceId")
          Long audienceId) {
    return ErrorHandler.handleAsync(
        audienceService.getAudienceDetails(tenantId, projectId, audienceId), "getAudienceDetails");
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
      @Parameter(description = "Tenant identifier", required = true) @HeaderParam("X-Tenant-Id")
          String tenantId,
      @Parameter(description = "Project identifier", required = true) @HeaderParam("X-Project-Id")
          String projectId,
      @Parameter(description = "ID of the audience", required = true) @PathParam("audienceId")
          Long audienceId,
      CreateRulesRequest requestBody) {

    requestBody.setAudienceId(audienceId);
    return ErrorHandler.handleAsync(
        audienceService.createRules(tenantId, projectId, requestBody), "createRules");
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
      @Parameter(description = "Tenant identifier", required = true) @HeaderParam("X-Tenant-Id")
          String tenantId,
      @Parameter(description = "Project identifier", required = true) @HeaderParam("X-Project-Id")
          String projectId,
      @Parameter(description = "ID of the audience", required = true) @PathParam("audienceId")
          Long audienceId,
      @Parameter(description = "ID of the rule to retrieve", required = true) @PathParam("ruleId")
          Long ruleId) {
    return ErrorHandler.handleAsync(
        audienceService.getRuleDetails(tenantId, projectId, audienceId, ruleId), "getRuleDetails");
  }

  @GET
  @Path("/v1/audiences")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List audiences with filters",
      description =
          "Retrieves a list of audiences with basic metadata including rule counts. Supports filtering by name, creator, and verification status.")
  @ApiResponse(
      responseCode = "200",
      description = "Successfully retrieved audiences list",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid query parameters",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<PaginatedResponse<AudienceMetaResponse>>>
      getAudiencesList(
          @Parameter(description = "Tenant identifier", required = true) @HeaderParam("X-Tenant-Id")
              String tenantId,
          @Parameter(description = "Project identifier", required = true)
              @HeaderParam("X-Project-Id")
              String projectId,
          @Parameter(description = "Search audiences by name (partial match)")
              @QueryParam("nameSearch")
              String nameSearch,
          @Parameter(description = "Filter by creator username") @QueryParam("createdBy")
              String createdBy,
          @Parameter(description = "Filter by verification status") @QueryParam("verified")
              Boolean verified,
          @Parameter(description = "Number of items per page", example = "10")
              @QueryParam("pageSize")
              @DefaultValue("10")
              @Min(1)
              int pageSize,
          @Parameter(description = "Page number (0-indexed)", example = "0")
              @QueryParam("page")
              @DefaultValue("0")
              @Min(0)
              int page) {
    return ErrorHandler.handleAsync(
        audienceService.getAudiencesList(
            tenantId, projectId, nameSearch, createdBy, verified, page, pageSize),
        "getAudiencesList");
  }
}
