package com.ascend.flockr.dao;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.Tuple;
import java.util.List;
import java.util.Map;

public interface ReadOperation {

  Single<JsonObject> health();

  <T> Maybe<T> findOne(String preparedQuery, Function<Row, T> mapper);

  <T> Maybe<T> findOneUsingSimpleQuery(String query, Function<Row, T> rowMapper);

  <T> Maybe<T> findOne(String preparedQuery, Tuple tuple, Function<Row, T> mapper);

  <T> Single<List<T>> findMultiple(String preparedQuery, Function<Row, T> mapper);

  <T> Single<List<T>> findMultipleUsingSimpleQuery(String query, Function<Row, T> rowMapper);

  <T> Single<List<T>> findMultiple(String preparedQuery, Tuple tuple, Function<Row, T> mapper);

  Single<Map<String, Integer>> findSingleColumnGroupCount(String preparedQuery, Tuple tuple);

  Single<Boolean> exists(String preparedQuery, Tuple tuple, String columnAlias);
}
