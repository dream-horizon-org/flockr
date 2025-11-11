package com.ascend.flockr.rest;

import com.ascend.flockr.domain.dataconnectors.DataConnectorType;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import com.ascend.flockr.io.ResponseEntity;
import com.ascend.flockr.io.request.OnboardDataSinkRequest;
import com.ascend.flockr.io.response.PaginatedResponse;
import com.ascend.flockr.service.DataConnectorService;
import com.google.inject.Inject;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DataSourcesController {

  private final DataConnectorService service;

  @GET
  @Path("/v1/connectors/types")
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<ResponseEntity.Success<List<DataConnectorType>>> listTypes(
      @QueryParam("kind") @DefaultValue("SOURCE") String kind) {
    return service.listTypes(kind).map(ResponseEntity.Success::new).toCompletionStage();
  }

  @POST
  @Path("/v1/datasources/onboard")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<ResponseEntity.Success<DataSourceDetails>> onboardSource(
      com.ascend.flockr.io.request.OnboardDataSourceRequest request,
      @HeaderParam("email") String userEmail) {
    return service
        .onboardSource(request, userEmail)
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }

  @POST
  @Path("/v1/datasinks/onboard")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<ResponseEntity.Success<DataSinkDetails>> onboardSink(
      OnboardDataSinkRequest request, @HeaderParam("email") String userEmail) {
    return service
        .onboardSink(request, userEmail)
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }

  @GET
  @Path("/v1/datasources")
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSourceDetails>>> listSources(
      @QueryParam("pageSize") @Min(1) @DefaultValue("10") int pageSize,
      @QueryParam("pageNum") @Min(0) @DefaultValue("0") int pageNum) {
    return service
        .listSources(pageNum, pageSize)
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }

  @GET
  @Path("/v1/datasinks")
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSinkDetails>>> listSinks(
      @QueryParam("pageSize") @Min(1) @DefaultValue("10") int pageSize,
      @QueryParam("pageNum") @Min(0) @DefaultValue("0") int pageNum) {
    return service
        .listSinks(pageNum, pageSize)
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }
}
