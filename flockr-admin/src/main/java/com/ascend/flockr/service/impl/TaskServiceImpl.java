package com.ascend.flockr.service.impl;

import com.ascend.flockr.domain.task.enums.TaskType;
import com.ascend.flockr.dto.response.TaskResponse;
import com.ascend.flockr.gateway.JobSchedulerGateway;
import com.ascend.flockr.repository.TaskRepository;
import com.ascend.flockr.service.TaskService;
import com.google.inject.Inject;
import io.reactivex.Single;
import java.util.concurrent.CompletionStage;

public final class TaskServiceImpl implements TaskService {

  private final TaskRepository taskRepository;
  private final JobSchedulerGateway jobSchedulerGateway;

  @Inject
  public TaskServiceImpl(TaskRepository taskRepository, JobSchedulerGateway jobSchedulerGateway) {
    this.taskRepository = taskRepository;
    this.jobSchedulerGateway = jobSchedulerGateway;
  }

  @Override
  public CompletionStage<TaskResponse> triggerTask(
      Long taskId, String clientId, String userIdentifier) {
    return taskRepository
        .findTaskDetailsById(taskId)
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.NO_SUCH_TASK)))
        .filter(this::isTriggerable)
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.OPERATION_NOT_SUPPORTED)))
        .doOnSuccess(
            taskDefinition -> {
              TaskType type = taskDefinition.getType();
              if (taskDefinition.getCronExpression() != null
                  && taskDefinition.getStatus() != TaskStatus.Running) {
                // for rules created without start time
                jobServiceKeeper
                    .get(type)
                    .startJob(taskDefinition.getId(), new JobConfig(clientUser));
              } else {
                jobServiceKeeper.get(type).triggerJob(taskDefinition, new JobConfig(clientUser));
              }
            })
        .ignoreElement();
  }
}
