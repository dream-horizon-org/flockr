package com.ascend.flockr.service.admin;

import com.ascend.flockr.io.response.TaskInfo;
import com.ascend.flockr.model.task.Task;
import com.ascend.flockr.util.DateUtil;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import lombok.Getter;

@Getter
class TaskInfoMapper
    implements Function<Task, TaskInfo>,
        java.util.function.Function<Task, TaskInfo>,
        BiFunction<Task, TaskInfo, TaskInfo> {

  @Override
  public TaskInfo apply(Task task) {
    TaskInfo taskInfo = new TaskInfo();
    apply(task, taskInfo);
    return taskInfo;
  }

  @Override
  public TaskInfo apply(Task task, TaskInfo taskInfo) {
    taskInfo.setId(task.getId());
    taskInfo.setCohortId(task.getCohortId());
    taskInfo.setName(task.getName());
    taskInfo.setDescription(task.getDescription());
    if (task.getType() != null) {
      taskInfo.setType(task.getType().ref());
    }
    if (task.getRuleType() != null) {
      taskInfo.setRuleType(task.getRuleType());
    }
    taskInfo.setAction(task.getAction());
    if (task.getStartTime() != null) {
      taskInfo.setStartDate(task.getStartTime() * 1000);
    }
    if (task.getEndTime() != null) {
      taskInfo.setEndDate(task.getEndTime() * 1000);
    }
    taskInfo.setCronExpression(task.getCronExpression());
    taskInfo.setStatus(task.getStatus());
    taskInfo.setClient(task.getClient());
    taskInfo.setCreatedAt(DateUtil.toDateTimeString(task.getCreatedAt()));
    taskInfo.setCreatedBy(task.getCreatedBy());
    return taskInfo;
  }
}
