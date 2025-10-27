package com.ascend.flockr.dao.jobexecutionlog;

import com.ascend.flockr.model.task.ChronicJobMetadata;
import io.reactivex.functions.Function;
import io.vertx.reactivex.sqlclient.Row;

@Deprecated
public class ChronicJobMetadataMapper implements Function<Row, ChronicJobMetadata> {

  @Override
  public ChronicJobMetadata apply(Row row) {
    ChronicJobMetadata chronicJobMetadata = new ChronicJobMetadata();
    chronicJobMetadata.setJobReferenceId(row.getLong("job_reference_id"));
    chronicJobMetadata.setTaskId(row.getLong(Column.TASK_ID));
    chronicJobMetadata.setNextExecutionTime(row.getLong("next_execution_time"));
    chronicJobMetadata.setRequestId(row.getString(Column.REQUEST_ID));
    return chronicJobMetadata;
  }
}
