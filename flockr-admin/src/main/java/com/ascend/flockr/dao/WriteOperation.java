package com.ascend.flockr.dao;

import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Tuple;

public interface WriteOperation extends ReadOperation {

  Single<Integer> insert(String preparedQuery, Tuple tuple);

  Single<Integer> update(String preparedQuery, Tuple tuple);

  Single<Integer> delete(String preparedQuery, Tuple tuple);

}
