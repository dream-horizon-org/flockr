package com.ascend.flockr.repository.impl;

import com.ascend.flockr.domain.task.Task;
import com.ascend.flockr.repository.TaskRepository;
import com.ascend.flockr.repository.internal.TaskReadRepository;
import com.ascend.flockr.repository.internal.TaskWriteRepository;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Maybe;

public final class TaskRepositoryFacade implements TaskRepository {

  private final TaskReadRepository readRepository;
  private final TaskWriteRepository writeRepository;

  @Inject
  public TaskRepositoryFacade(
      TaskReadRepository readRepository, TaskWriteRepository writeRepository) {
    this.readRepository = readRepository;
    this.writeRepository = writeRepository;
  }

  @Override
  public Maybe<Task> findTaskDetailsById(Long id) {
    return readRepository.findTaskDetailsById(id);
  }
}
