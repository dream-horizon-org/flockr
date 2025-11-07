package com.ascend.flockr.repository.internal.impl;

import com.ascend.flockr.domain.task.Task;
import com.ascend.flockr.repository.internal.TaskReadRepository;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Maybe;

public final class TaskReadRepositoryImpl implements TaskReadRepository {

  @Inject
  public TaskReadRepositoryImpl() {}

  @Override
  public Maybe<Task> findTaskDetailsById(long id) {
    return Maybe.empty();
  }
}
