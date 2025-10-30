package com.ascend.flockr.rest;

import com.ascend.flockr.config.AppConfig;
import com.ascend.flockr.config.ClientConfig;
import com.ascend.flockr.io.response.Response;
import com.ascend.flockr.io.response.TaskExecutionDetailResponse;
import com.ascend.flockr.io.response.TaskInfoVerbose;
import com.ascend.flockr.service.AdminOperation;
import com.ascend.flockr.util.ResponseWrapper;
import com.ascend.flockr.util.RestResponseUtil;
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

  @GET
  @Path("/tasks/{id}/jobs")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response<TaskExecutionDetailResponse<?>>> taskExecutionDetail(
      @NotNull @PathParam("id") Long taskId) {
    return adminOps.taskExecutionDetail(taskId).to(RestResponseUtil.jaxrsRestHandler());
  }
}
