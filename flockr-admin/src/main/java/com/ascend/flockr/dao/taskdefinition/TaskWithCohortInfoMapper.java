package com.ascend.flockr.dao.taskdefinition;

import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import io.reactivex.functions.Function;
import io.vertx.reactivex.sqlclient.Row;

public class TaskWithCohortInfoMapper implements Function<Row, TaskWithCohortInfo> {

    @Override
    public TaskWithCohortInfo apply(Row row) throws Exception {
        TaskWithCohortInfo taskWithCohortInfo = new TaskWithCohortInfo();
        taskWithCohortInfo.setId(row.getLong(Column.ID));
        taskWithCohortInfo.setCohortId(
                row.getLong(Column.COHORT_ID));
        taskWithCohortInfo.setType(
                TaskType.valueOf(row.getString(Column.TYPE)));
        taskWithCohortInfo.setStartTime(
                row.getLong(Column.START_TIME));
        taskWithCohortInfo.setEndTime(
                row.getLong(Column.END_TIME));
        taskWithCohortInfo.setCronExpression(
                row.getString(Column.CRON_EXPRESSION));
        taskWithCohortInfo.setStatus(
                TaskStatus.valueOf(
                        row.getString(Column.STATUS)));
        taskWithCohortInfo.setExpired(row.getBoolean(Column.IS_EXPIRED));
        taskWithCohortInfo.setCohortExpiry(row.getLong(Column.EXPIRATION_DATE));
        return taskWithCohortInfo;
    }
}