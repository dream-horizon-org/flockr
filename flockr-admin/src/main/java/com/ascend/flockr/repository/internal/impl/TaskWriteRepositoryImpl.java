package com.ascend.flockr.repository.internal.impl;

import com.ascend.flockr.domain.task.Task;
import com.ascend.flockr.repository.internal.TaskWriteRepository;
import com.ascend.flockr.repository.sql.TaskSql;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.sqlclient.Tuple;

public final class TaskWriteRepositoryImpl implements TaskWriteRepository {

  private final DatabaseExecutor executor;

  @Inject
  public TaskWriteRepositoryImpl(DatabaseExecutor executor) {
    this.executor = executor;
  }

  public Completable save(Task task) {
    // TODO: Build tuple from task
    return executor.execute(TaskSql.INSERT_TASK, buildTuple(task));
  }

  private Tuple buildTuple(Task task) {
    // TODO: Implement tuple building from task
    return Tuple.of();
  }
}
