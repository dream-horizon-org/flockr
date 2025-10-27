package com.ascend.flockr.dao.jobexecutionlog;

import com.ascend.flockr.model.task.JobExecutionLog;
import io.reactivex.functions.Function;
import io.vertx.reactivex.sqlclient.Row;

class JobExecutionLogMapper implements Function<Row, JobExecutionLog> {

  @Override
  public JobExecutionLog apply(Row row) throws Exception {
    JobExecutionLog jobExecutionLog = new JobExecutionLog();
    jobExecutionLog.setId(row.getString(Column.ID));
    jobExecutionLog.setTaskId(row.getLong(Column.TASK_ID));
    jobExecutionLog.setJarId(row.getString(Column.JAR_ID));
    jobExecutionLog.setState(row.getString(Column.STATE));
    jobExecutionLog.setRequestId(row.getString(Column.REQUEST_ID));
    jobExecutionLog.setSynced(row.getBoolean(Column.SYNCED));
    jobExecutionLog.setWatermark(row.getLong(Column.WATERMARK));
    return jobExecutionLog;
  }
}
