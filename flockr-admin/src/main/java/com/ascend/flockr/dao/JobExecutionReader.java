package com.ascend.flockr.dao;

import com.ascend.flockr.model.task.ChronicJobMetadata;
import com.ascend.flockr.model.task.JobExecutionLog;
import com.ascend.flockr.model.task.SavepointInfo;
import com.ascend.flockr.model.task.TaskExecutionDetail;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;

public interface JobExecutionReader {

  Maybe<JobExecutionLog> findById(String jobId);

  Maybe<JobExecutionLog> findLatestByTaskId(Long taskId);

  Single<List<TaskExecutionDetail>> findLatest10ByTaskId(Long taskId);

  Maybe<String> findLatestSavepoint(Long taskId);

  Single<List<SavepointInfo>> findAllSavepointByTaskId(Long taskId);

  Maybe<Long> findLatestJobReferenceIdByTaskId(Long taskId);

  @Deprecated
  Single<List<ChronicJobMetadata>> findAllChronicJobMetadataByTaskId(Long taskId);

  @Deprecated
  Maybe<ChronicJobMetadata> findChronicJobMetadataByJobReferenceId(Long jobReferenceId);

  @Deprecated
  Single<List<ChronicJobMetadata>> findChronicJobMetadataWithPastExecutionTime();

  Single<List<String>> findIdWithStateRunningAndStartTimeDiff(Long thresholdTimeInMinutes);
}
