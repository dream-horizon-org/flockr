package com.ascend.flockr.dao.jobexecutionlog;

import com.ascend.flockr.model.task.SavepointInfo;
import io.reactivex.functions.Function;
import io.vertx.reactivex.sqlclient.Row;

public class SavepointInfoMapper implements Function<Row, SavepointInfo> {

  @Override
  public SavepointInfo apply(Row row) throws Exception {
    SavepointInfo savepointInfo = new SavepointInfo();
    savepointInfo.setId(row.getLong("id"));
    savepointInfo.setTaskId(row.getLong("task_id"));
    savepointInfo.setJobId(row.getString("job_id"));
    savepointInfo.setSavepointPath(row.getString("savepoint_path"));
    savepointInfo.setCreatedAt(row.getLong("created_at"));
    return savepointInfo;
  }
}
