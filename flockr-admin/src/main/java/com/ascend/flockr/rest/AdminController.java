package com.ascend.flockr.rest;

import com.ascend.flockr.config.AppConfig;
import com.ascend.flockr.config.ClientConfig;
import com.ascend.flockr.exception.DefinedException;
import com.ascend.flockr.exception.ErrorEntity;
import com.ascend.flockr.io.request.UpdateTaskRequest;
import com.ascend.flockr.io.response.Response;
import com.ascend.flockr.io.response.SubmitRuleResponse;
import com.ascend.flockr.io.response.TaskExecutionDetailResponse;
import com.ascend.flockr.io.response.TaskInfoVerbose;
import com.ascend.flockr.service.AdminOperation;
import com.ascend.flockr.util.ResponseWrapper;
import com.ascend.flockr.util.RestResponseUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class AdminController {

  private final Map<String, ClientConfig> authorisedClient;
  private final AdminOperation adminOps;

  public AdminController(AppConfig appConfig, AdminOperation adminOperation) {
    this.authorisedClient =
        appConfig.getAuth().getClient().stream()
            .collect(Collectors.toMap(ClientConfig::getId, i -> i));
    this.adminOps = adminOperation;
  }

  @GET
  @Path("/tasks/{id}")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response<TaskInfoVerbose>> taskInfo(
      @NotNull @PathParam("id") Long taskId) {
    return ResponseWrapper.fromMaybe(adminOps.findTaskInfoVerboseById(taskId), null, 200);
  }

  @PATCH
  @Path("/tasks/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response<Object>> update(
      @NotNull @PathParam("id") Long taskId,
      @NotNull @Valid UpdateTaskRequest request,
      @HeaderParam("clientId") String clientId,
      @HeaderParam("msd-user-email") String userEmail) {

    if (clientId == null || !authorisedClient.containsKey(clientId) || userEmail == null) {
      throw new DefinedException(ErrorEntity.USER_UNAUTHORIZED);
    }
    request.validate();
    String client = authorisedClient.get(clientId).getAlias();
    return ResponseWrapper.fromCompletable(
        adminOps.updateTask(taskId, request, client, userEmail), "task updated successfully", 200);
  }

  @DELETE
  @Path("/tasks/{id}")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response<Object>> delete(
      @NotNull @PathParam("id") Long taskId,
      @HeaderParam("clientId") String clientId,
      @HeaderParam("clientUser") String userIdentifier) {

    if (clientId == null || !authorisedClient.containsKey(clientId)) {
      throw new DefinedException(ErrorEntity.USER_UNAUTHORIZED);
    }

    String client = authorisedClient.get(clientId).getAlias();
    String clientUser = userIdentifier != null ? userIdentifier : client;
    return ResponseWrapper.fromCompletable(
        adminOps.deleteTask(taskId, client, clientUser), null, 202);
  }

  @GET
  @Path("/tasks/{id}/jobs")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response<TaskExecutionDetailResponse<?>>> taskExecutionDetail(
      @NotNull @PathParam("id") Long taskId) {
    return adminOps.taskExecutionDetail(taskId).to(RestResponseUtil.jaxrsRestHandler());
  }

  @POST
  @Path("/tasks/{id}/trigger")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response<SubmitRuleResponse>> triggerRule(
      @NotNull @PathParam("id") Long taskId,
      @HeaderParam("clientId") String clientId,
      @HeaderParam("clientUser") String userIdentifier) {

    if (clientId == null || !authorisedClient.containsKey(clientId)) {
      throw new DefinedException(ErrorEntity.USER_UNAUTHORIZED);
    }

    String client = authorisedClient.get(clientId).getAlias();
    String clientUser = userIdentifier != null ? userIdentifier : client;
    return ResponseWrapper.fromCompletable(adminOps.trigger(taskId, client, clientUser), null, 201);
  }

  @PUT
  @Path("/tasks/{id}/pause")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response<Object>> pause(
      @NotNull @PathParam("id") Long taskId,
      @HeaderParam("clientId") String clientId,
      @HeaderParam("clientUser") String userIdentifier) {

    if (clientId == null || !authorisedClient.containsKey(clientId)) {
      throw new DefinedException(ErrorEntity.USER_UNAUTHORIZED);
    }

    String client = authorisedClient.get(clientId).getAlias();
    String clientUser = userIdentifier != null ? userIdentifier : client;
    return ResponseWrapper.fromCompletable(adminOps.pause(taskId, client, clientUser), null, 202);
  }

  @PUT
  @Path("/tasks/{id}/resume")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response<Object>> resume(
      @NotNull @PathParam("id") Long taskId,
      @HeaderParam("clientId") String clientId,
      @HeaderParam("clientUser") String userIdentifier) {

    if (clientId == null || !authorisedClient.containsKey(clientId)) {
      throw new DefinedException(ErrorEntity.USER_UNAUTHORIZED);
    }

    String client = authorisedClient.get(clientId).getAlias();
    String clientUser = userIdentifier != null ? userIdentifier : client;
    return ResponseWrapper.fromCompletable(adminOps.resume(taskId, client, clientUser), null, 202);
  }

  @PUT
  @Path("/tasks/{id}/terminate")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response<Object>> terminate(
      @NotNull @PathParam("id") Long taskId,
      @HeaderParam("clientId") String clientId,
      @HeaderParam("msd-user-email") String userIdentifier) {

    if (clientId == null || !authorisedClient.containsKey(clientId)) {
      throw new DefinedException(ErrorEntity.USER_UNAUTHORIZED);
    }

    String client = authorisedClient.get(clientId).getAlias();
    return ResponseWrapper.fromCompletable(
        adminOps.terminate(taskId, client, userIdentifier), null, 202);
  }
}
