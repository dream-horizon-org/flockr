package com.ascend.flockr.service;

import com.ascend.flockr.service.flink.CheckpointStatistics;
import com.ascend.flockr.service.flink.JobDetailResponse;
import com.ascend.flockr.service.flink.JobsOverviewResponse;
import com.ascend.flockr.service.flink.SavepointStatusResponse;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface FlinkClient {

  Single<JobDetailResponse> jobDetail(String jobId);

  Single<JobsOverviewResponse> jobsOverview();

  Single<CheckpointStatistics> checkpointStatistics(String jobId);

  Single<String> submitJob(Long taskId, String jarId);

  Single<String> submitJob(Long taskId, String jarId, String savepointPath);

  Single<String> triggerSavepointAndCancelJob(String jobId);

  Single<SavepointStatusResponse> savepointStatus(String jobId, String triggerId);

  Completable terminateJob(String jobId);
}
