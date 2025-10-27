package com.ascend.flockr.dao.jobexecutionlog;

import com.ascend.flockr.model.task.TaskExecutionDetail;
import com.ascend.flockr.util.JsonColumnUtil;
import com.ascend.flockr.util.stacktraceparser.ErrorLog;
import io.reactivex.functions.Function;
import io.vertx.reactivex.sqlclient.Row;

public class TaskExecutionDetailMapper implements Function<Row, TaskExecutionDetail> {

  @Override
  public TaskExecutionDetail apply(Row row) {
    TaskExecutionDetail executionLog = new TaskExecutionDetail();
    executionLog.setJobId(row.getString(Column.ID));
    executionLog.setTaskId(row.getLong(Column.TASK_ID));
    executionLog.setJarId(row.getString(Column.JAR_ID));
    executionLog.setStartTime(row.getLong(Column.START_TIME));
    executionLog.setEndTime(row.getLong(Column.END_TIME));
    executionLog.setState(row.getString(Column.STATE));
    executionLog.setRequestId(row.getString(Column.REQUEST_ID));
    executionLog.setSynced(row.getBoolean(Column.SYNCED));
    executionLog.setWatermark(row.getLong(Column.WATERMARK));
    executionLog.setCreatedAt(row.getLong(Column.CREATED_AT));
    executionLog.setUpdatedAt(row.getLong(Column.UPDATED_AT));
    executionLog.setErrorLog(JsonColumnUtil.get(row, Column.ERROR_LOG, ErrorLog.class));
    executionLog.setRecordsProcessed(row.getLong(Column.RECORDS_PROCESSED));
    return executionLog;
  }
}
