package com.ascend.flockr.dao.taskdefinition;

import com.ascend.flockr.io.request.HistoricSequence;
import com.ascend.flockr.io.request.RealTimeSequence;
import com.ascend.flockr.model.task.TaskDefinition;
import com.ascend.flockr.model.task.constant.TaskType;
import com.ascend.flockr.model.task.rule.Rule;
import com.ascend.flockr.util.JsonColumnUtil;
import io.reactivex.functions.Function;
import io.vertx.reactivex.sqlclient.Row;
import lombok.SneakyThrows;

class TaskDefinitionMapper implements Function<Row, TaskDefinition> {

  private final TaskMapper taskMapper = new TaskMapper();

  @Override
  @SneakyThrows
  public TaskDefinition apply(Row row) {
    TaskDefinition taskDefinition = new TaskDefinition();
    taskMapper.apply(row, taskDefinition);
    if (taskDefinition.getType() == TaskType.eventStream) {
      taskDefinition.setSequence(JsonColumnUtil.get(row, Column.SEQUENCE, RealTimeSequence.class));
    } else if (taskDefinition.getType() == TaskType.storedData) {
      taskDefinition.setSequence(JsonColumnUtil.get(row, Column.SEQUENCE, HistoricSequence.class));
    }
    taskDefinition.setRule(JsonColumnUtil.get(row, Column.RULE, Rule.class));
    taskDefinition.setUpdatedAt(row.getLong(Column.UPDATED_AT));
    taskDefinition.setUpdatedBy(row.getString(Column.UPDATED_BY));
    taskDefinition.setResumeUsingLatestArtifact(
        row.getBoolean(Column.RESUME_USING_LATEST_ARTIFACT));
    taskDefinition.setResumeFromSavepoint(row.getBoolean(Column.RESUME_FROM_SAVEPOINT));
    return taskDefinition;
  }
}
