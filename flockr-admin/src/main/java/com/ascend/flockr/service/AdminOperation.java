package com.ascend.flockr.service;

import com.ascend.flockr.io.response.TaskExecutionDetailResponse;
import com.ascend.flockr.io.response.TaskInfoVerbose;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;

public interface AdminOperation {

  Maybe<TaskInfoVerbose> findTaskInfoVerboseById(Long taskId);

  Single<List<TaskInfoVerbose>> findAllTaskByCohortId(Long cohortId);

  Single<TaskExecutionDetailResponse<?>> taskExecutionDetail(Long taskId);
}
