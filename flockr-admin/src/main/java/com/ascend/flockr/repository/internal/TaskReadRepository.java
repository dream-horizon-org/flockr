package com.ascend.flockr.repository.internal;

import com.ascend.flockr.domain.task.Task;
import io.reactivex.rxjava3.core.Maybe;

public interface TaskReadRepository {
  Maybe<Task> findTaskDetailsById(long id);
}
