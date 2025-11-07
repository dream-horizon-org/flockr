package com.ascend.flockr.repository.executor;

import io.reactivex.rxjava3.core.Maybe;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowIterator;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.function.Function;

public class QueryExecutor {
  private final PostgresReaderClient readerClient;

  public <T> Maybe<T> fetchOne(String preparedQuery, Tuple tuple, Function<Row, T> rowMapper) {
    return readerClient
        .rxExecute(preparedQuery, tuple)
        .map(RowSet::iterator)
        .filter(RowIterator::hasNext)
        .map(RowIterator::next)
        .map(rowMapper);
  }
}
