package com.ascend.flockr.dao;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.Tuple;
import java.util.List;

public interface ReadOperation {

  <T> Maybe<T> findOne(String preparedQuery, Tuple tuple, Function<Row, T> mapper);

  <T> Single<List<T>> findMultiple(String preparedQuery, Function<Row, T> mapper);

  <T> Single<List<T>> findMultiple(String preparedQuery, Tuple tuple, Function<Row, T> mapper);
}
