package com.ascend.flockr.service;

import com.ascend.flockr.dto.response.TaskResponse;
import java.util.concurrent.CompletionStage;

public interface TaskService {
  public CompletionStage<TaskResponse> triggerTask(
      Long taskId, String clientId, String userIdentifier);
}
