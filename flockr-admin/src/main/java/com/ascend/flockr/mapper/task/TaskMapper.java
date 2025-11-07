package com.ascend.flockr.mapper.task;

import com.ascend.flockr.domain.rule.enums.RuleAction;
import com.ascend.flockr.domain.rule.enums.RuleType;
import com.ascend.flockr.domain.task.Task;
import com.ascend.flockr.domain.task.enums.TaskStatus;
import com.ascend.flockr.domain.task.enums.TaskType;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.vertx.rxjava3.sqlclient.Row;

class TaskMapper implements Function<Row, Task>, BiFunction<Row, Task, Task> {

  @Override
  public Task apply(Row row) {
    Task task = new Task();
    return apply(row, task);
  }

  @Override
  public Task apply(Row row, Task task) {
    task.getTaskBaseInfo().setId(row.getLong(Column.ID));
    task.getTaskBaseInfo().setCohortId(row.getLong(Column.COHORT_ID));
    task.getTaskBaseInfo().setName(row.getString(Column.NAME));
    task.getTaskBaseInfo().setDescription(row.getString(Column.DESCRIPTION));
    task.getTaskBaseInfo().setStartTime(row.getLong(Column.START_TIME));
    task.getTaskBaseInfo().setEndTime(row.getLong(Column.END_TIME));
    task.getTaskBaseInfo().setCronExpression(row.getString(Column.CRON_EXPRESSION));
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
    task.getTaskBaseInfo().setClient(row.getString(Column.CLIENT));
    task.getTaskBaseInfo().setCreatedAt(row.getLong(Column.CREATED_AT));
    task.getTaskBaseInfo().setCreatedBy(row.getString(Column.CREATED_BY));
    return task;
  }
}
