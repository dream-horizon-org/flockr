package com.ascend.flockr.dao;

import com.ascend.flockr.dao.taskdefinition.TaskWithJobInfo;
import com.ascend.flockr.model.task.Task;
import com.ascend.flockr.model.task.TaskDefinition;
import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;
import javax.swing.*;

public interface TaskDefinitionReader {

  //  Maybe<Task> findById(Long id);

  Maybe<TaskDefinition> findDetailById(Long id);

  Maybe<TaskStatus> findStatusById(Long id);

  Single<List<TaskDefinition>> findAllByCohortId(Long cohortId);

  Maybe<Task> findById(Long id);

  //  Single<List<Task>> findAllByStatus(
  //      List<TaskStatus> statusList,
  //      RuleType ruleType,
  //      String sort,
  //      SortOrder order,
  //      Integer limit,
  //      Integer offset);
  //
  //  Single<List<TaskDefinition>> findAllByStatusAndTypeAndUpdatedAtDiff(
  //      List<TaskStatus> statusList, TaskType type, Long updatedAtDiffInSeconds);
  //
  //
  //  Single<List<TaskDefinition>> findLatest10ByTaskName(String Name);
  //
  //  Single<Integer> findCountByStatus(List<TaskStatus> statusList, RuleType ruleType);
  //
  //  Single<Map<String, Integer>> findCountByGroup(String groupingParam, RuleType ruleType);

  Single<List<TaskWithJobInfo>> findAllWithLatestJobByStatusAndType(
      List<TaskStatus> statusList, TaskType type);

  //  Single<List<TaskWithJobInfo>> findAllWithActiveJobsByTypeAndJobStateAndUpdatedAtDiff(
  //      TaskType type, List<String> activeJobState, Long updatedAtDiffInSeconds);
  //
  //  Single<List<TaskDefinition>> findAllTasksWithEndTimeLessThanCurrentTimeAndStatusIn(
  //      Set<TaskStatus> terminableTaskStatus);
  //
  //  Single<List<TaskDefinition>> findAllWithStartTimeLessThanCurrentTimeAndStatusScheduled();
  //
  //  Single<List<CronTrigger>> findAllCronWithStatusActiveAndPastNextExecutionTime();
  //
  //  Maybe<CronTrigger> findCronByTaskIdAndStatusActive(Long taskId);
  //
  //  Single<List<CronTrigger>> findCronByTaskId(Long taskId);
  //
  //  Maybe<Boolean> findIfCohortOwnerExists(Long cohortId, String owner);
  //
  //  Single<Boolean> findIfTaskExistsByCohortId(Long cohortId);
}
