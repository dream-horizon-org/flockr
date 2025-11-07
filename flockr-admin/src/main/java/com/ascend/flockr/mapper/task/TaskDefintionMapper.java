package com.ascend.flockr.mapper.task;

import com.ascend.flockr.domain.rule.Rule;
import com.ascend.flockr.domain.task.TaskDefinition;
import com.ascend.flockr.domain.task.enums.TaskType;
import com.ascend.flockr.util.JsonColumnUtil;
import io.vertx.rxjava3.sqlclient.Row;
import java.util.function.Function;
import lombok.SneakyThrows;

class TaskDefinitionMapper implements Function<Row, TaskDefinition> {

  private final TaskMapper taskMapper = new TaskMapper();

  @Override
  @SneakyThrows
  public TaskDefinition apply(Row row) {
    TaskDefinition taskDefinition = new TaskDefinition();
    taskMapper.apply(row, taskDefinition);
    if (taskDefinition.getType() == TaskType.eventStream) {
      //            taskDefinition.setSequence(JsonColumnUtil.get(row, Column.SEQUENCE,
      // RealTimeSequence.class));
    } else if (taskDefinition.getType() == TaskType.storedData) {
      //            taskDefinition.setSequence(JsonColumnUtil.get(row, Column.SEQUENCE,
      // HistoricSequence.class));
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
