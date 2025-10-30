package com.ascend.flockr.dao.taskdefinition;

import com.ascend.flockr.model.task.constant.RuleAction;
import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import io.reactivex.functions.Function;
import io.vertx.reactivex.sqlclient.Row;

public class TaskWithJobInfoMapper implements Function<Row, TaskWithJobInfo> {

  @Override
  public TaskWithJobInfo apply(Row row) {
    TaskWithJobInfo result = new TaskWithJobInfo();
    result.setId(row.getLong(Column.ID));
    result.setName(row.getString(Column.NAME));
    if (row.getString(Column.TYPE) != null) {
      result.setType(TaskType.valueOf(row.getString(Column.TYPE)));
    }
    if (row.getString(Column.RULE_ACTION) != null) {
      result.setAction(RuleAction.valueOf(row.getString(Column.RULE_ACTION)));
    }
    result.setCronExpression(row.getString(Column.CRON_EXPRESSION));
    if (row.getString(Column.STATUS) != null) {
      result.setTaskStatus(TaskStatus.valueOf(row.getString(Column.STATUS)));
    }
    result.setTaskUpdatedAt(row.getLong(Column.UPDATED_AT));
    result.setJobId(row.getString(ALIAS.JOB_ID));
    result.setJobCreatedAt(row.getLong(ALIAS.JOB_CREATED_AT));
    result.setJobUpdatedAt(row.getLong(ALIAS.JOB_UPDATED_AT));
    result.setJobStatus(row.getString(ALIAS.JOB_STATE));
    return result;
  }
}
