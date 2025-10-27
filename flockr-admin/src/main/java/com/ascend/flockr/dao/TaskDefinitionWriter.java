package com.ascend.flockr.dao;

import com.ascend.flockr.dao.taskdefinition.TaskSchedule;
import com.ascend.flockr.model.crontrigger.CronTrigger;
import com.ascend.flockr.model.crontrigger.CronTriggerStatus;
import com.ascend.flockr.model.task.TaskDefinition;
import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;

public interface TaskDefinitionWriter extends TaskDefinitionReader {

  Single<Long> create(TaskDefinition taskDefinition);

  Single<List<TaskDefinition>> createMultipleTaskForCohort(
      List<TaskDefinition> taskDefinitions, Long cohortId);

  Single<List<TaskDefinition>> createMultipleTask(List<TaskDefinition> taskDefinitions);

  Single<Boolean> updateTaskSchedule(Long id, TaskSchedule schedule, String updatedBy);

  Single<Boolean> updateTaskSchedule(
      Long id, TaskSchedule schedule, String updatedBy, CronTrigger cronTrigger);

  Single<Boolean> updateEndDateAndStatus(
      Long id, TaskType type, TaskStatus status, String updatedBy, Long endDateInSeconds);

  Single<Boolean> updateStatus(Long id, TaskType type, TaskStatus status);

  Single<Boolean> updateStatusAndUpdatedBy(
      Long id, TaskType type, TaskStatus status, String updatedBy);

  Single<Boolean> updateStatusByIdAndUpdatedAt(
      Long id, TaskType type, TaskStatus status, Long updatedAt);

  Single<Boolean> updateColumn(Long taskId, String columnName, Object columnValue);

  Single<Boolean> deleteDraft(Long id);

  Single<Integer> insertCron(CronTrigger cronTrigger);

  Single<Boolean> updateCronByIdAndUpdatedAt(
      Long id, Long updatedAt, String requestId, Long nextExecutionTime);

  Single<Boolean> updateCronStatusByTaskIdAndStatus(
      Long taskId, CronTriggerStatus finalStatus, CronTriggerStatus currentStatus);

  Single<Boolean> updateTask(Long taskId, Map<String, Object> pathValueMapping);
}
