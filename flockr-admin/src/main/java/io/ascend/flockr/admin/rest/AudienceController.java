package io.ascend.flockr.admin.rest;

import com.google.inject.Inject;
import io.ascend.flockr.admin.io.ResponseEntity;
import io.ascend.flockr.admin.io.request.CreateAudienceRequest;
import io.ascend.flockr.admin.io.request.CreateRulesRequest;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerRequest;
import io.ascend.flockr.admin.io.response.AudienceDetailsResponse;
import io.ascend.flockr.admin.io.response.AudienceMetaResponse;
import io.ascend.flockr.admin.io.response.AudienceOwnerResponse;
import io.ascend.flockr.admin.io.response.PaginatedResponse;
import io.ascend.flockr.admin.io.response.RuleDetailsResponse;
import io.ascend.flockr.admin.service.AudienceService;
import io.ascend.flockr.admin.util.ErrorHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing audiences and their associated rules.
 *
 * <p>This controller provides endpoints for:
 *
 * <ul>
 *   <li>Creating and retrieving audiences
 *   <li>Creating and retrieving rules for audiences
 *   <li>Listing audiences with filtering and pagination
 * </ul>
 *
 * <p>All endpoints require encrypted project identifier via X-Project-Id header.
 *
 * @author Prithu Sharma
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
      @Parameter(
              description = "Encrypted project identifier (amalgamation of tenantId and projectId)",
              required = true)
          @HeaderParam("X-Project-Id")
          String xProjectId,
      @Parameter(
              description = "Actor email/username (defaults to 'system' if not provided)",
              required = false)
          @HeaderParam("email")
          String actor,
      @Valid CreateAudienceRequest requestBody) {
    return ErrorHandler.handleAsync(
        audienceService.createAudience(xProjectId, requestBody, actor), "createAudience");
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
      @Parameter(description = "Encrypted project identifier", required = true)
          @HeaderParam("X-Project-Id")
          String xProjectId,
      @Parameter(description = "ID of the audience to retrieve", required = true)
          @PathParam("audienceId")
          Long audienceId) {
    return ErrorHandler.handleAsync(
        audienceService.getAudienceDetails(xProjectId, audienceId), "getAudienceDetails");
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
      @Parameter(description = "Encrypted project identifier", required = true)
          @HeaderParam("X-Project-Id")
          String xProjectId,
      @Parameter(
              description = "Actor email/username (defaults to 'system' if not provided)",
              required = false)
          @HeaderParam("email")
          String actor,
      @Parameter(description = "ID of the audience", required = true) @PathParam("audienceId")
          Long audienceId,
      CreateRulesRequest requestBody) {

    requestBody.setAudienceId(audienceId);
    return ErrorHandler.handleAsync(
        audienceService.createRules(xProjectId, requestBody, actor), "createRules");
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
      @Parameter(description = "Encrypted project identifier", required = true)
          @HeaderParam("X-Project-Id")
          String xProjectId,
      @Parameter(description = "ID of the audience", required = true) @PathParam("audienceId")
          Long audienceId,
      @Parameter(description = "ID of the rule to retrieve", required = true) @PathParam("ruleId")
          Long ruleId) {
    return ErrorHandler.handleAsync(
        audienceService.getRuleDetails(xProjectId, audienceId, ruleId), "getRuleDetails");
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
          @Parameter(description = "Encrypted project identifier", required = true)
              @HeaderParam("X-Project-Id")
              String xProjectId,
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
            xProjectId, nameSearch, createdBy, verified, page, pageSize),
        "getAudiencesList");
  }

  @GET
  @Path("/v1/audiences/{audienceId}/owners")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get all audience owners",
      description = "Retrieves all owners (active and inactive) for a specific audience")
  @ApiResponse(
      responseCode = "200",
      description = "Successfully retrieved audience owners",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid/missing parameters",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Audience not found",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<java.util.List<AudienceOwnerResponse>>>
      getAudienceOwners(
          @Parameter(description = "Encrypted project identifier", required = true)
              @HeaderParam("X-Project-Id")
              String xProjectId,
          @Parameter(description = "ID of the audience", required = true) @PathParam("audienceId")
              Long audienceId) {
    return ErrorHandler.handleAsync(
        audienceService.getAudienceOwners(xProjectId, audienceId), "getAudienceOwners");
  }

  @POST
  @Path("/v1/audiences/{audienceId}/owners")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Add or remove an audience owner",
      description =
          "Adds or removes an owner for a specific audience. The acting user must be an existing owner.")
  @ApiResponse(
      responseCode = "200",
      description = "Audience owner updated successfully",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid/missing parameters or expired audience",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden: user is not authorized to update audience owners",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Conflict: owner update failed",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<Void> updateAudienceOwner(
      @Parameter(description = "Encrypted project identifier", required = true)
          @HeaderParam("X-Project-Id")
          String xProjectId,
      @Parameter(description = "ID of the audience whose owner is to be updated", required = true)
          @PathParam("audienceId")
          Long audienceId,
      @Parameter(
              description = "Email of the logged-in user (defaults to 'system' if not provided)",
              required = false)
          @HeaderParam("email")
          String email,
      @Valid UpdateAudienceOwnerRequest requestBody) {
    return audienceService
        .updateAudienceOwner(xProjectId, audienceId, email, requestBody)
        .toCompletionStage(null);
  }
}
