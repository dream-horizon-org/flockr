package com.ascend.flockr.service.web;

import com.ascend.flockr.io.response.TaskExecutionDetailResponse;
import com.ascend.flockr.model.task.Task;
import com.ascend.flockr.model.task.TaskDefinition;
import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.rule.Rule;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Promise;
import java.util.List;

public interface AsyncJobService<T> {

  Single<Boolean> isValid(Rule rule);

  void startJob(Long taskId, JobConfig config);

  /* This will trigger new job at every invocation */
  void triggerJob(TaskDefinition task, JobConfig config);

  void pauseJob(Long taskId);

  void resumeJob(Long taskId, ResumeJobConfig config);

  void terminateJob(Long taskId);

  Single<TaskExecutionDetailResponse<?>> taskExecutionDetail(Task task);

  void maintenance(Long taskId);

  void maintenance(List<TaskStatus> maintainableTaskStatus, Promise<Void> completePromise);

  Maybe<Long> findTaskIdByJobRefId(String jobId);

  default boolean validUser(String user) {
    return !user.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$\n");
  }
}
