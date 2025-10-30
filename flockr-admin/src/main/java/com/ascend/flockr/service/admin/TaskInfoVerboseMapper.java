package com.ascend.flockr.service.admin;

import com.ascend.flockr.io.response.TaskInfoVerbose;
import com.ascend.flockr.model.task.TaskDefinition;
import com.ascend.flockr.util.DateUtil;
import io.reactivex.functions.Function;
import lombok.SneakyThrows;

class TaskInfoVerboseMapper
    implements Function<TaskDefinition, TaskInfoVerbose>,
        java.util.function.Function<TaskDefinition, TaskInfoVerbose> {

  private final TaskInfoMapper taskInfoMapper = new TaskInfoMapper();

  @Override
  @SneakyThrows
  public TaskInfoVerbose apply(TaskDefinition taskDefinition) {
    TaskInfoVerbose taskInfoVerbose = new TaskInfoVerbose();
    taskInfoMapper.apply(taskDefinition, taskInfoVerbose);
    taskInfoVerbose.setSequence(taskDefinition.getSequence());
    taskInfoVerbose.setRule(taskDefinition.getRule());
    taskInfoVerbose.setUpdatedAt(DateUtil.toDateTimeString(taskDefinition.getUpdatedAt()));
    taskInfoVerbose.setUpdatedBy(taskDefinition.getUpdatedBy());
    taskInfoVerbose.setResumeUsingLatestArtifact(taskDefinition.getResumeUsingLatestArtifact());
    taskInfoVerbose.setResumeFromSavepoint(taskDefinition.getResumeFromSavepoint());
    return taskInfoVerbose;
  }
}
