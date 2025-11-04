package com.ascend.flockr.service;

import com.ascend.flockr.io.request.UpdateTaskRequest;
import com.ascend.flockr.io.response.TaskExecutionDetailResponse;
import com.ascend.flockr.io.response.TaskInfoVerbose;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;

public interface AdminOperation {

  Maybe<TaskInfoVerbose> findTaskInfoVerboseById(Long taskId);

  Single<List<TaskInfoVerbose>> findAllTaskByCohortId(Long cohortId);

  Single<TaskExecutionDetailResponse<?>> taskExecutionDetail(Long taskId);

  Completable trigger(Long taskId, String client, String clientUser);

  Completable pause(Long taskId, String client, String clientUser);

  Completable resume(Long taskId, String client, String clientUser);

  Completable terminate(Long taskId, String client, String clientUser);

  Completable updateTask(Long taskId, UpdateTaskRequest request, String client, String clientUser);

  Completable deleteTask(Long taskId, String client, String clientUser);


}
