package com.ascend.flockr.service.admin;

import com.ascend.flockr.dao.TaskDefinitionReader;
import com.ascend.flockr.exception.DefinedException;
import com.ascend.flockr.exception.ErrorEntity;
import com.ascend.flockr.io.response.TaskExecutionDetailResponse;
import com.ascend.flockr.io.response.TaskInfoVerbose;
import com.ascend.flockr.model.task.TaskDefinition;
import com.ascend.flockr.model.task.constant.TaskType;
import com.ascend.flockr.service.AdminOperation;
import com.ascend.flockr.service.web.AsyncJobService;
import com.ascend.flockr.util.ListTypeTransformer;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;

public class AdminOperationImpl implements AdminOperation {

  private final TaskDefinitionReader taskDefinitionReader;
  private final TaskInfoVerboseMapper taskInfoVerboseMapper = new TaskInfoVerboseMapper();
  private final ListTypeTransformer<TaskDefinition, TaskInfoVerbose> taskDefinitionListTransformer =
      new ListTypeTransformer<>();
  private final Map<TaskType, AsyncJobService<?>> jobServiceKeeper;

    public AdminOperationImpl(TaskDefinitionReader taskDefinitionReader, Map<TaskType, AsyncJobService<?>> jobServiceKeeper) {
    this.taskDefinitionReader = taskDefinitionReader;
    this.jobServiceKeeper = jobServiceKeeper;
    }

  public Maybe<TaskInfoVerbose> findTaskInfoVerboseById(Long taskId) {
    return taskDefinitionReader.findDetailById(taskId).map(taskInfoVerboseMapper);
  }

  @Override
  public Single<List<TaskInfoVerbose>> findAllTaskByCohortId(Long cohortId) {
    return taskDefinitionReader
        .findAllByCohortId(cohortId)
        .zipWith(Single.just(taskInfoVerboseMapper), taskDefinitionListTransformer);
  }

  @Override
  public Single<TaskExecutionDetailResponse<?>> taskExecutionDetail(Long taskId) {
    return taskDefinitionReader
        .findById(taskId)
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.NO_SUCH_TASK)))
        .flatMap(task -> jobServiceKeeper.get(task.getType()).taskExecutionDetail(task));
  }
}
