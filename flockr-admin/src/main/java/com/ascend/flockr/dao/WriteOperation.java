package com.ascend.flockr.dao;

import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Tuple;
import java.util.List;

public interface WriteOperation extends ReadOperation {

  Single<Long> insertAndGenerateId(String preparedQuery, Tuple tuple);

  Single<Integer> insert(String preparedQuery, Tuple tuple);

  Single<Integer> insertMultiple(String preparedQuery, List<Tuple> tuples);

  Single<Integer> update(String preparedQuery, Tuple tuple);

  Single<Integer> updateMultiple(String preparedQuery, List<Tuple> tuples);

  Single<Integer> update(String preparedQuery);

  Single<Integer> delete(String preparedQuery, Tuple tuple);
}
