package com.ascend.flockr.dao;

import com.ascend.flockr.dao.taskdefinition.TaskWithCohortInfo;
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

  Maybe<TaskDefinition> findDetailById(Long id);

  Maybe<TaskStatus> findStatusById(Long id);

  Single<List<TaskDefinition>> findAllByCohortId(Long cohortId);

  Maybe<Task> findById(Long id);

  Maybe<Boolean> findIfCohortOwnerExists(Long cohortId, String owner);

  Maybe<TaskWithCohortInfo> findTaskWithCohortInfoById(Long taskId);

  Single<List<TaskWithJobInfo>> findAllWithLatestJobByStatusAndType(
      List<TaskStatus> statusList, TaskType type);

}
