package com.ascend.flockr.repository;

import com.ascend.flockr.domain.task.Task;
import io.reactivex.rxjava3.core.Maybe;

public interface TaskRepository {

  // Reads
  Maybe<Task> findTaskDetailsById(Long id);
}
