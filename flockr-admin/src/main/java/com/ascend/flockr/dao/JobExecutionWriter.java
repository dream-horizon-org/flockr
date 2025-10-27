package com.ascend.flockr.dao;

import com.ascend.flockr.model.task.ChronicJobMetadata;
import com.ascend.flockr.model.task.JobExecutionLog;
import com.ascend.flockr.model.task.JobExecutionLogDetail;
import com.ascend.flockr.model.task.constant.TaskType;
import com.ascend.flockr.util.stacktraceparser.ErrorLog;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;

public interface JobExecutionWriter extends JobExecutionReader {

  Completable create(JobExecutionLog jobExecutionLog, TaskType taskType);

  Completable upsert(JobExecutionLogDetail jobExecutionLogDetail, TaskType taskType);

  Single<Boolean> update(
      String jobId,
      Long taskId,
      TaskType taskType,
      String state,
      Long startTimeInMillis,
      Long endTimeInMillis);

  Completable updateByUpdatedAt(
      String jobId,
      Long taskId,
      TaskType taskType,
      String state,
      Long startTimeInMillis,
      Long endTimeInMillis,
      Long updatedAt);

  Single<Boolean> updateState(String jobId, Long taskId, TaskType taskType, String state);

  Single<Boolean> updateStateByUpdatedAt(
      String jobId,
      Long taskId,
      TaskType taskType,
      String state,
      Long updatedAt,
      ErrorLog errorLog,
      Long recordsProcessed);

  Completable updatedSyncedTrueByRequestIdIn(List<String> requestIds);

  Completable updatedMultipleSyncedTrueAndWatermarkById(Map<String, Long> jobWatermarks);

  Completable addSavepoint(Long taskId, String jobId, String savepointPath);

  @Deprecated
  Completable addChronicJobMetadata(ChronicJobMetadata chronicJobMetadata);

  Single<Boolean> updateExecutionTime(Long jobReferenceId, Long nextExecutionTime);
}
