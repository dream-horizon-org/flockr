package com.ascend.flockr.dao.taskdefinition;

import com.ascend.flockr.model.task.Task;
import com.ascend.flockr.model.task.constant.RuleAction;
import com.ascend.flockr.model.task.constant.RuleType;
import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.vertx.reactivex.sqlclient.Row;

class TaskMapper implements Function<Row, Task>, BiFunction<Row, Task, Task> {

  @Override
  public Task apply(Row row) {
    Task task = new Task();
    return apply(row, task);
  }

  @Override
  public Task apply(Row row, Task task) {
    task.setId(row.getLong(Column.ID));
    task.setCohortId(row.getLong(Column.COHORT_ID));
    task.setName(row.getString(Column.NAME));
    task.setDescription(row.getString(Column.DESCRIPTION));
    task.setStartTime(row.getLong(Column.START_TIME));
    task.setEndTime(row.getLong(Column.END_TIME));
    task.setCronExpression(row.getString(Column.CRON_EXPRESSION));
    if (row.getString(Column.TYPE) != null) {
      task.setType(TaskType.valueOf(row.getString(Column.TYPE)));
    }
    if (row.getString(Column.RULE_TYPE) != null) {
      task.setRuleType(RuleType.valueOf(row.getString(Column.RULE_TYPE)));
    }
    if (row.getString(Column.RULE_ACTION) != null) {
      task.setAction(RuleAction.valueOf(row.getString(Column.RULE_ACTION)));
    }
    if (row.getString(Column.STATUS) != null) {
      task.setStatus(TaskStatus.valueOf(row.getString(Column.STATUS)));
    }
    task.setClient(row.getString(Column.CLIENT));
    task.setCreatedAt(row.getLong(Column.CREATED_AT));
    task.setCreatedBy(row.getString(Column.CREATED_BY));
    return task;
  }
}
