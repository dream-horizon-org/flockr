package com.ascend.flockr.dao.jobexecutionlog;

import com.ascend.flockr.dao.JobExecutionWriter;
import com.ascend.flockr.model.task.ChronicJobMetadata;
import com.ascend.flockr.model.task.JobExecutionLog;
import com.ascend.flockr.model.task.JobExecutionLogDetail;
import com.ascend.flockr.model.task.constant.TaskType;
import com.ascend.flockr.util.JsonColumnUtil;
import com.ascend.flockr.util.MetricUtil;
import com.ascend.flockr.util.stacktraceparser.ErrorLog;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.timgroup.statsd.StatsDClient;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.reactivex.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class JobExecutionMasterRepo extends JobExecutionSlaveRepo implements JobExecutionWriter {

  private static final String INSERT_JOB_EXECUTION_LOG =
      "INSERT INTO job_execution_log (id, task_id, jar_id, state) VALUES (?, ?, ?, ?)";

  private static final String UPSERT_JOB_EXECUTION_LOG_DETAIL =
      "INSERT INTO job_execution_log (id, task_id, jar_id, start_time, end_time, state, request_id, error_log, records_processed) VALUES "
          + "(?, ?, ?, FROM_UNIXTIME (?), FROM_UNIXTIME (?), ?, ?, ?, ?) ON DUPLICATE KEY "
          + "UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time), state = VALUES(state), error_log = VALUES(error_log), records_processed = VALUES(records_processed)";

  private static final String UPDATE_JOB_EXECUTION_LOG =
      "UPDATE job_execution_log SET state = ?, start_time = FROM_UNIXTIME (FLOOR(? / 1000)), end_time = FROM_UNIXTIME (FLOOR(? / 1000)) WHERE id = ?";

  private static final String UPDATE_JOB_EXECUTION_LOG_BY_UPDATED_AT =
      "UPDATE job_execution_log SET state = ?, start_time = FROM_UNIXTIME (FLOOR(? / 1000)), end_time = FROM_UNIXTIME (FLOOR(? / 1000)) WHERE id = ? AND updated_at = FROM_UNIXTIME(?)";

  private static final String UPDATE_JOB_EXECUTION_LOG_STATE =
      "UPDATE job_execution_log SET state = ? WHERE id = ?";

  private static final String UPDATE_JOB_EXECUTION_LOG_STATE_AND_ERROR_BY_UPDATED_AT =
      "UPDATE job_execution_log SET state = ?, error_log = ?, records_processed = ? WHERE id = ? AND updated_at = FROM_UNIXTIME(?)";

  private static final String UPDATE_JEL_SYNCED_TRUE_BY_REQUEST_ID =
      "UPDATE job_execution_log SET synced = TRUE WHERE request_id = ?";

  private static final String UPDATE_JEL_SYNCED_TRUE_AND_WATERMARK_BY_ID =
      "UPDATE job_execution_log SET synced = TRUE, watermark = FROM_UNIXTIME(?) WHERE id = ?";

  private static final String INSERT_SAVEPOINT_INFO =
      "INSERT INTO savepoint_info (task_id, job_id, savepoint_path) VALUES (?, ?, ?)";

  private static final String INSERT_CHRONIC_JOB_METADATA =
      "INSERT INTO chronic_job_metadata (job_reference_id, task_id, next_execution_time, request_id) VALUES (?, ?, FROM_UNIXTIME (?), ?)";

  private static final String UPDATE_NEXT_EXECUTION_TIME =
      "UPDATE chronic_job_metadata SET next_execution_time = FROM_UNIXTIME (?) WHERE job_reference_id = ?";

  private final StatsDClient d11DDClient;

  @Inject
  public JobExecutionMasterRepo(
      @Named("mysql-client-nucleus") MySQLClient client, StatsDClient d11DDClient) {
    super(client);
    this.d11DDClient = d11DDClient;
  }

  @Override
  public Completable create(JobExecutionLog jobExecutionLog, TaskType taskType) {
    Tuple tuple = Tuple.tuple();
    tuple.addValue(jobExecutionLog.getId());
    tuple.addValue(jobExecutionLog.getTaskId());
    tuple.addValue(jobExecutionLog.getJarId());
    tuple.addValue(jobExecutionLog.getState());
    return insert(INSERT_JOB_EXECUTION_LOG, tuple)
        .doOnSuccess(
            rowCount -> {
              if (rowCount > 0 && jobExecutionLog.getState().equals("FAILED")) {
                incrementFailureCounter(
                    jobExecutionLog.getId(), jobExecutionLog.getTaskId(), taskType);
              }
            })
        .ignoreElement();
  }

  @Override
  public Completable upsert(JobExecutionLogDetail jobExecutionLogDetail, TaskType taskType) {
    Tuple tuple = Tuple.tuple();
    tuple.addValue(jobExecutionLogDetail.getId());
    tuple.addValue(jobExecutionLogDetail.getTaskId());
    tuple.addValue(jobExecutionLogDetail.getJarId());
    tuple.addValue(jobExecutionLogDetail.getStartTime());
    tuple.addValue(jobExecutionLogDetail.getEndTime());
    tuple.addValue(jobExecutionLogDetail.getState());
    tuple.addValue(jobExecutionLogDetail.getRequestId());
    tuple.addValue(JsonColumnUtil.set(jobExecutionLogDetail.getErrorLog()));
    tuple.addValue(jobExecutionLogDetail.getRecordsProcessed());
    return insert(UPSERT_JOB_EXECUTION_LOG_DETAIL, tuple)
        .doOnSuccess(
            rowCount -> {
              if (rowCount > 0 && jobExecutionLogDetail.getState().equals("FAILED")) {
                incrementFailureCounter(
                    jobExecutionLogDetail.getId(), jobExecutionLogDetail.getTaskId(), taskType);
              }
            })
        .ignoreElement();
  }

  @Override
  public Single<Boolean> update(
      String jobId,
      Long taskId,
      TaskType taskType,
      String state,
      Long startTimeInMillis,
      Long endTimeInMillis) {
    return update(
            UPDATE_JOB_EXECUTION_LOG, Tuple.of(state, startTimeInMillis, endTimeInMillis, jobId))
        .map(rowsAffected -> rowsAffected > 0)
        .doOnSuccess(
            success -> {
              if (success && state.equals("FAILED")) {
                incrementFailureCounter(jobId, taskId, taskType);
              }
            });
  }

  @Override
  public Completable updateByUpdatedAt(
      String jobId,
      Long taskId,
      TaskType taskType,
      String state,
      Long startTimeInMillis,
      Long endTimeInMillis,
      Long updatedAt) {
    return update(
            UPDATE_JOB_EXECUTION_LOG_BY_UPDATED_AT,
            Tuple.of(state, startTimeInMillis, endTimeInMillis, jobId, updatedAt))
        .map(rowsAffected -> rowsAffected > 0)
        .doOnSuccess(
            success -> {
              if (success && state.equals("FAILED")) {
                incrementFailureCounter(jobId, taskId, taskType);
              }
            })
        .ignoreElement();
  }

  @Override
  public Single<Boolean> updateState(String jobId, Long taskId, TaskType taskType, String state) {
    return update(UPDATE_JOB_EXECUTION_LOG_STATE, Tuple.of(state, jobId))
        .map(rowsAffected -> rowsAffected > 0)
        .doOnSuccess(
            success -> {
              if (success && state.equals("FAILED")) {
                incrementFailureCounter(jobId, taskId, taskType);
              }
            });
  }

  @Override
  public Single<Boolean> updateStateByUpdatedAt(
      String jobId,
      Long taskId,
      TaskType taskType,
      String state,
      Long updatedAt,
      ErrorLog errorLog,
      Long recordsProcessed) {
    return update(
            UPDATE_JOB_EXECUTION_LOG_STATE_AND_ERROR_BY_UPDATED_AT,
            Tuple.of(state, JsonColumnUtil.set(errorLog), recordsProcessed, jobId, updatedAt))
        .map(rowsAffected -> rowsAffected > 0)
        .doOnSuccess(
            success -> {
              if (success && state.equals("FAILED")) {
                incrementFailureCounter(jobId, taskId, taskType);
              }
            });
  }

  @Override
  public Completable updatedSyncedTrueByRequestIdIn(List<String> requestIds) {
    return rxExecuteBatch(
            UPDATE_JEL_SYNCED_TRUE_BY_REQUEST_ID,
            requestIds.stream().map(Tuple::of).collect(Collectors.toList()))
        .ignoreElement();
  }

  @Override
  public Completable updatedMultipleSyncedTrueAndWatermarkById(Map<String, Long> jobWatermarks) {
    List<Tuple> tuples = new ArrayList<>();
    jobWatermarks.forEach((k, v) -> tuples.add(Tuple.of(v, k)));
    return rxExecuteBatch(UPDATE_JEL_SYNCED_TRUE_AND_WATERMARK_BY_ID, tuples).ignoreElement();
  }

  @Override
  public Completable addSavepoint(Long taskId, String jobId, String savepointPath) {
    return insert(INSERT_SAVEPOINT_INFO, Tuple.of(taskId, jobId, savepointPath)).ignoreElement();
  }

  @Override
  @Deprecated
  public Completable addChronicJobMetadata(ChronicJobMetadata chronicJobMetadata) {
    Tuple tuple = Tuple.tuple();
    tuple.addValue(chronicJobMetadata.getJobReferenceId());
    tuple.addValue(chronicJobMetadata.getTaskId());
    tuple.addValue(chronicJobMetadata.getNextExecutionTime());
    tuple.addValue(chronicJobMetadata.getRequestId());
    return insert(INSERT_CHRONIC_JOB_METADATA, tuple).ignoreElement();
  }

  @Override
  public Single<Boolean> updateExecutionTime(Long jobReferenceId, Long nextExecutionTime) {
    return update(UPDATE_NEXT_EXECUTION_TIME, Tuple.of(nextExecutionTime, jobReferenceId))
        .map(rowsAffected -> rowsAffected > 0);
  }

  private void incrementFailureCounter(String jobId, Long taskId, TaskType taskType) {
    d11DDClient.increment(
        MetricUtil.executionFailedAspect(),
        "job_id:" + jobId,
        "task_id:" + taskId,
        "task_type:" + taskType);
  }
}
