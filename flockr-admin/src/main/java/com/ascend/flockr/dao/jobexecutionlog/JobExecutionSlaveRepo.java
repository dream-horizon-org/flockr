package com.ascend.flockr.dao.jobexecutionlog;

import com.ascend.flockr.dao.AbstractRepository;
import com.ascend.flockr.dao.JobExecutionReader;
import com.ascend.flockr.model.task.ChronicJobMetadata;
import com.ascend.flockr.model.task.JobExecutionLog;
import com.ascend.flockr.model.task.SavepointInfo;
import com.ascend.flockr.model.task.TaskExecutionDetail;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.reactivex.mysqlclient.MySQLPool;
import io.vertx.reactivex.sqlclient.Tuple;
import java.util.List;

class JobExecutionSlaveRepo extends AbstractRepository implements JobExecutionReader {

  private static final String FIND_BY_ID =
      "SELECT id, task_id, jar_id, state, request_id, synced, UNIX_TIMESTAMP (watermark) AS watermark FROM job_execution_log WHERE id = ?";

  private static final String FIND_LATEST_BY_TASK_ID =
      "SELECT id, task_id, jar_id, state, request_id, synced, UNIX_TIMESTAMP (watermark) AS watermark FROM job_execution_log WHERE task_id = ? ORDER BY created_at DESC LIMIT 1";

  private static final String FIND_LATEST_10_BY_TASK_ID =
      "SELECT id, task_id, jar_id, UNIX_TIMESTAMP (start_time) AS start_time, UNIX_TIMESTAMP (end_time) AS end_time, state, request_id, "
          + "synced, UNIX_TIMESTAMP (watermark) AS watermark, UNIX_TIMESTAMP (created_at) AS created_at, UNIX_TIMESTAMP (updated_at) AS "
          + "updated_at, error_log, records_processed FROM job_execution_log WHERE task_id = ? ORDER BY created_at DESC LIMIT 10";

  private static final String FIND_LATEST_SAVEPOINT =
      "SELECT savepoint_path FROM savepoint_info WHERE task_id = ? ORDER BY created_at DESC LIMIT 1";

  private static final String FIND_ALL_SAVEPOINT_BY_TASK_ID =
      "SELECT id, task_id, job_id, savepoint_path, UNIX_TIMESTAMP (created_at) AS created_at "
          + "FROM savepoint_info WHERE task_id = ? ORDER BY created_at DESC";

  private static final String FIND_LATEST_JOB_REFERENCE_ID =
      "SELECT job_reference_id FROM chronic_job_metadata WHERE task_id = ? ORDER BY created_at DESC LIMIT 1";

  private static final String FIND_ALL_CHRONIC_JOB_METADATA_BY_TASK_ID =
      "SELECT job_reference_id, task_id, UNIX_TIMESTAMP (next_execution_time) AS next_execution_time FROM chronic_job_metadata "
          + "WHERE task_id = ? ORDER BY created_at DESC";
  private static final String FIND_CHRONIC_JOB_METADATA_BY_JOB_REFERENCE_ID =
      "SELECT job_reference_id, task_id, UNIX_TIMESTAMP (next_execution_time) AS next_execution_time, request_id FROM chronic_job_metadata "
          + "WHERE job_reference_id = ?";

  private static final String FIND_CHRONIC_JOB_METADATA_WITH_PAST_EXECUTION_TIME =
      "SELECT job_reference_id, task_id, UNIX_TIMESTAMP (next_execution_time) AS next_execution_time FROM chronic_job_metadata WHERE next_execution_time < NOW()";

  private static final String FIND_ID_WITH_STATE_RUNNING_AND_START_TIME_DIFF =
      "SELECT id FROM job_execution_log WHERE state = 'RUNNING' AND jar_id IS NULL AND TIMESTAMPDIFF(MINUTE, start_time, NOW()) > ?";

  private final JobExecutionLogMapper jobExecutionLogMapper = new JobExecutionLogMapper();
  private final TaskExecutionDetailMapper taskExecutionDetailMapper =
      new TaskExecutionDetailMapper();
  private final SavepointInfoMapper savepointInfoMapper = new SavepointInfoMapper();

  @Deprecated
  private final ChronicJobMetadataMapper chronicJobMetadataMapper = new ChronicJobMetadataMapper();

  @Inject
  public JobExecutionSlaveRepo(@Named("mysql-client-nucleus") MySQLClient client) {
    super((MySQLPool) client);
  }

  public JobExecutionSlaveRepo(MySQLPool mySQLPool) {
    super(mySQLPool);
  }

  @Override
  public Maybe<JobExecutionLog> findById(String jobId) {
    return findOne(FIND_BY_ID, Tuple.of(jobId), jobExecutionLogMapper);
  }

  @Override
  public Maybe<JobExecutionLog> findLatestByTaskId(Long taskId) {
    return findOne(FIND_LATEST_BY_TASK_ID, Tuple.of(taskId), jobExecutionLogMapper);
  }

  @Override
  public Single<List<TaskExecutionDetail>> findLatest10ByTaskId(Long taskId) {
    return findMultiple(FIND_LATEST_10_BY_TASK_ID, Tuple.of(taskId), taskExecutionDetailMapper);
  }

  @Override
  public Maybe<String> findLatestSavepoint(Long taskId) {
    return findOne(FIND_LATEST_SAVEPOINT, Tuple.of(taskId), row -> row.getString("savepoint_path"));
  }

  @Override
  public Single<List<SavepointInfo>> findAllSavepointByTaskId(Long taskId) {
    return findMultiple(FIND_ALL_SAVEPOINT_BY_TASK_ID, Tuple.of(taskId), savepointInfoMapper);
  }

  @Override
  public Maybe<Long> findLatestJobReferenceIdByTaskId(Long taskId) {
    return findOne(
        FIND_LATEST_JOB_REFERENCE_ID, Tuple.of(taskId), row -> row.getLong("job_reference_id"));
  }

  @Override
  @Deprecated
  public Single<List<ChronicJobMetadata>> findAllChronicJobMetadataByTaskId(Long taskId) {
    return findMultiple(
        FIND_ALL_CHRONIC_JOB_METADATA_BY_TASK_ID, Tuple.of(taskId), chronicJobMetadataMapper);
  }

  @Override
  @Deprecated
  public Maybe<ChronicJobMetadata> findChronicJobMetadataByJobReferenceId(Long jobReferenceId) {
    return findOne(
        FIND_CHRONIC_JOB_METADATA_BY_JOB_REFERENCE_ID,
        Tuple.of(jobReferenceId),
        chronicJobMetadataMapper);
  }

  @Override
  @Deprecated
  public Single<List<ChronicJobMetadata>> findChronicJobMetadataWithPastExecutionTime() {
    return findMultiple(
        FIND_CHRONIC_JOB_METADATA_WITH_PAST_EXECUTION_TIME, chronicJobMetadataMapper);
  }

  @Override
  public Single<List<String>> findIdWithStateRunningAndStartTimeDiff(Long thresholdTimeInMinutes) {
    return findMultiple(
        FIND_ID_WITH_STATE_RUNNING_AND_START_TIME_DIFF,
        Tuple.of(thresholdTimeInMinutes),
        row -> row.getString(Column.ID));
  }
}
