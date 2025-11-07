package com.ascend.flockr.rest;

import com.ascend.flockr.dto.ResponseEntity;
import com.ascend.flockr.dto.response.TaskResponse;
import com.ascend.flockr.service.TaskService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/v1/tasks")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TasksController {

  private final TaskService taskService;

  @POST
  @Path("/{id}/trigger")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<TaskResponse> triggerRule(
      @NotNull @PathParam("id") Long taskId,
      @HeaderParam("clientId") String clientId,
      @HeaderParam("clientUser") String userIdentifier) {

    if (clientId == null || !authorisedClient.containsKey(clientId)) {
      throw new DefinedException(ResponseEntity.Failure.ErrorEntity.USER_UNAUTHORIZED);
    }

    String client = authorisedClient.get(clientId).getAlias();
    String clientUser = userIdentifier != null ? userIdentifier : client;
    return ResponseWrapper.fromCompletable(
        taskService.triggerTask(taskId, client, clientUser), null, 201);
  }
}
