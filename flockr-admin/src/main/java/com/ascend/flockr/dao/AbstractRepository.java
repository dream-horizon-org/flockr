package com.ascend.flockr.dao;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.reactivex.mysqlclient.MySQLPool;
import io.vertx.reactivex.sqlclient.*;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRepository implements ReadOperation, WriteOperation {

  private static final String HEALTH_CHECK_QUERY = "SELECT 1";
  private final MySQLPool client;

  public AbstractRepository(MySQLPool client) {
    this.client = client;
  }

  protected Single<RowSet<Row>> rxExecute(String preparedQuery) {
    return client
        .preparedQuery(preparedQuery)
        .rxExecute()
        .doOnError(Throwable::printStackTrace)
        .retry(1, this::retryable);
  }

  protected Single<RowSet<Row>> rxExecuteSimpleQuery(String query) {
    return client
        .query(query)
        .rxExecute()
        .doOnError(Throwable::printStackTrace)
        .retry(1, this::retryable);
  }

  protected Single<RowSet<Row>> rxExecute(String preparedQuery, Tuple tuple) {
    return client
        .preparedQuery(preparedQuery)
        .rxExecute(tuple)
        .doOnError(Throwable::printStackTrace)
        .retry(1, this::retryable);
  }

  protected Single<RowSet<Row>> rxExecuteBatch(String preparedQuery, List<Tuple> tuples) {
    return client
        .preparedQuery(preparedQuery)
        .rxExecuteBatch(tuples)
        .doOnError(Throwable::printStackTrace)
        .retry(1, this::retryable);
  }

  protected MySQLPool client() {
    return client;
  }

  @Override
  public <T> Maybe<T> findOne(String preparedQuery, Tuple tuple, Function<Row, T> rowMapper) {
    return rxExecute(preparedQuery, tuple)
        .map(RowSet::iterator)
        .filter(RowIterator::hasNext)
        .map(RowIterator::next)
        .map(rowMapper);
  }

  @Override
  public <T> Single<List<T>> findMultiple(String preparedQuery, Function<Row, T> rowMapper) {
    return rxExecute(preparedQuery).map(rows -> toList(rows, rowMapper));
  }

  @Override
  public <T> Single<List<T>> findMultiple(
      String preparedQuery, Tuple tuple, Function<Row, T> rowMapper) {
    return rxExecute(preparedQuery, tuple).map(rows -> toList(rows, rowMapper));
  }

  @Override
  public Single<Integer> insert(String preparedQuery, Tuple tuple) {
    return rxExecute(preparedQuery, tuple).map(SqlResult::rowCount);
  }

  /**
   * @return emits no. of rows matched not affected
   */
  @Override
  public Single<Integer> update(String preparedQuery, Tuple tuple) {
    return rxExecute(preparedQuery, tuple).map(SqlResult::rowCount);
  }

  private Boolean checkIfExists(RowSet<Row> rows, String columnAlias) {
    Row row = rows.iterator().next();
    return row.getBoolean(columnAlias);
  }

  @SneakyThrows
  protected <T> List<T> toList(RowSet<Row> rows, Function<Row, T> rowMapper) {
    List<T> collection = new LinkedList<>();
    for (Row row : rows) {
      T t = rowMapper.apply(row);
      collection.add(t);
    }
    return collection;
  }

  protected Map<String, Integer> toSingleColumnGroupCountMap(RowSet<Row> rows) {
    Map<String, Integer> countMap = new HashMap<>();
    for (Row row : rows) {
      countMap.put(row.getString(0), row.getInteger(1));
    }
    return countMap;
  }

  private boolean retryable(Throwable exception) {
    if (exception instanceof ConnectException) {
      log.error("error occurred while attempting to connect a socket, retrying db query...");
      return true;
    } else {
      return false;
    }
  }

    @Override
    public Single<Integer> delete(String preparedQuery, Tuple tuple) {
        return rxExecute(preparedQuery, tuple).map(SqlResult::rowCount);
    }

    @Override
    public Single<Long> insertAndGenerateId(String preparedQuery, Tuple tuple) {
        return rxExecute(preparedQuery, tuple)
                .map(result -> result.property(new PropertyKind<>(MySQLClient.LAST_INSERTED_ID)));
    }
}
