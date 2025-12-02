package io.ascend.flockr.admin.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.ascend.flockr.admin.domain.dataconnectors.DataConnectorType;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.io.ResponseEntity;
import io.ascend.flockr.admin.io.request.OnboardConnectorTypeRequest;
import io.ascend.flockr.admin.io.request.OnboardDataSinkRequest;
import io.ascend.flockr.admin.io.request.OnboardDataSourceRequest;
import io.ascend.flockr.admin.io.response.PaginatedResponse;
import io.ascend.flockr.admin.service.DataConnectorService;
import io.ascend.flockr.admin.util.ErrorHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing data connectors (sources and sinks).
 *
 * <p>This controller provides endpoints for:
 *
 * <ul>
 *   <li>Onboarding connector types, data sources, and data sinks
 *   <li>Listing available connector types
 *   <li>Listing data sources and sinks with pagination
 * </ul>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Path("/")
@Tag(name = "Data Connectors", description = "Data source and sink management APIs")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DataSourcesController {
  private final DataConnectorService service;
  private final ObjectMapper mapper;

  @POST
  @Path("/v1/connectors/types/onboard")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Onboard a connector type",
      description = "Onboards a new data connector type (SOURCE or SINK)")
  @ApiResponse(
      responseCode = "200",
      description = "Connector type onboarded successfully",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid/missing body params",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<DataConnectorType>> onboardConnectorType(
      @Valid OnboardConnectorTypeRequest request,
      @Parameter(description = "User email for authentication", required = true)
          @HeaderParam("email")
          String userEmail) {

    return ErrorHandler.handleAsync(
        service.onboardConnectorType(request, userEmail), "onboardConnectorType");
  }

  @GET
  @Path("/v1/connectors/types")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List connector types",
      description = "Retrieves a list of available data connector types")
  @ApiResponse(
      responseCode = "200",
      description = "Successful Response",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<List<DataConnectorType>>> listTypes(
      @Parameter(description = "Kind of connector (SOURCE or SINK)", example = "SOURCE")
          @QueryParam("kind")
          @DefaultValue("SOURCE")
          String kind) {
    return ErrorHandler.handleAsync(service.listTypes(kind), "listTypes");
  }

  @GET
  @Path("/v1/connectors/types/{typeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get connector type by ID",
      description = "Retrieves all details of an onboarded source or sink connector type by typeId")
  @ApiResponse(
      responseCode = "200",
      description = "Successful Response",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Connector type not found",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid typeId",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<DataConnectorType>> getConnectorTypeById(
      @Parameter(description = "ID of the connector type", required = true, example = "1")
          @PathParam("typeId")
          Long typeId) {
    return ErrorHandler.handleAsync(service.getConnectorTypeById(typeId), "getConnectorTypeById");
  }

  @POST
  @Path("/v1/datasources/onboard")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Onboard a data source",
      description = "Onboards a new data source connector")
  @ApiResponse(
      responseCode = "200",
      description = "Data source onboarded successfully",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid/missing body params",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<DataSourceDetails>> onboardSource(
      @Valid OnboardDataSourceRequest request,
      @Parameter(description = "User email for authentication", required = true)
          @HeaderParam("email")
          String userEmail) {
    return ErrorHandler.handleAsync(service.onboardSource(request, userEmail), "onboardSource");
  }

  @POST
  @Path("/v1/datasinks/onboard")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Onboard a data sink", description = "Onboards a new data sink connector")
  @ApiResponse(
      responseCode = "200",
      description = "Data sink onboarded successfully",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid/missing body params",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<DataSinkDetails>> onboardSink(
      @Valid OnboardDataSinkRequest request,
      @Parameter(description = "User email for authentication", required = true)
          @HeaderParam("email")
          String userEmail) {
    return ErrorHandler.handleAsync(service.onboardSink(request, userEmail), "onboardSink");
  }

  @GET
  @Path("/v1/datasources")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List data sources",
      description = "Retrieves a paginated list of data sources")
  @ApiResponse(
      responseCode = "200",
      description = "Successful Response",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid pagination parameters",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSourceDetails>>> listSources(
      @Parameter(description = "Number of items per page", example = "10")
          @QueryParam("pageSize")
          @Min(1)
          @DefaultValue("10")
          int pageSize,
      @Parameter(description = "Page number (0-indexed)", example = "0")
          @QueryParam("pageNum")
          @Min(0)
          @DefaultValue("0")
          int pageNum) {
    return ErrorHandler.handleAsync(service.listSources(pageNum, pageSize), "listSources");
  }

  @GET
  @Path("/v1/datasinks")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "List data sinks", description = "Retrieves a paginated list of data sinks")
  @ApiResponse(
      responseCode = "200",
      description = "Successful Response",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request due to invalid pagination parameters",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSinkDetails>>> listSinks(
      @Parameter(description = "Number of items per page", example = "10")
          @QueryParam("pageSize")
          @Min(1)
          @DefaultValue("10")
          int pageSize,
      @Parameter(description = "Page number (0-indexed)", example = "0")
          @QueryParam("pageNum")
          @Min(0)
          @DefaultValue("0")
          int pageNum) {
    return ErrorHandler.handleAsync(service.listSinks(pageNum, pageSize), "listSinks");
  }
}
