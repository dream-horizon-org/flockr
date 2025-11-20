package com.ascend.flockr.client.postgres;

import com.ascend.flockr.config.PostgresConfig;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.rxjava3.sqlclient.SqlConnection;
import io.vertx.rxjava3.sqlclient.Tuple;
import io.vertx.sqlclient.PoolOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

public abstract class AbstractPostgresClient {

  @Getter private final PgPool postgresPool;
  private final Integer retryCount;

  protected AbstractPostgresClient(Vertx vertx, PostgresConfig.BaseConfig postgresBaseConfig) {
    this.postgresPool =
        PgPool.pool(
            vertx,
            getConnectOptions(postgresBaseConfig.getConnectOptions()),
            getPoolOptions(postgresBaseConfig.getPoolOptions()));
    this.retryCount = postgresBaseConfig.getRetryCount();
  }

  public Completable rxClose() {
    return this.postgresPool.rxClose();
  }

  protected Single<RowSet<Row>> rxExecute(String query) {
    return this.postgresPool.query(query).rxExecute().retry(this.retryCount);
  }

  protected Single<RowSet<Row>> rxExecute(String preparedQuery, Tuple tuple) {
    return this.postgresPool.preparedQuery(preparedQuery).rxExecute(tuple).retry(this.retryCount);
  }

  protected Single<RowSet<Row>> rxExecute(
      SqlConnection connection, String preparedQuery, Tuple tuple) {
    return connection.preparedQuery(preparedQuery).rxExecute(tuple);
  }

  protected Single<RowSet<Row>> rxExecute(
      SqlConnection connection, String preparedQuery, List<Tuple> tuples) {
    return connection.preparedQuery(preparedQuery).rxExecuteBatch(tuples);
  }

  protected <T> Maybe<T> rxWithTransaction(
      Function<SqlConnection, Maybe<T>> transactionalFunction) {
    return this.postgresPool.rxWithTransaction(transactionalFunction);
  }

  protected static <T> List<T> toList(RowSet<Row> rows, Function<Row, T> rowMapper) {
    List<T> resultList = new ArrayList<>();
    for (Row row : rows) {
      resultList.add(rowMapper.apply(row));
    }
    return resultList;
  }

  protected static <K, V> Map<K, V> toMap(
      RowSet<Row> rows, Function<Row, K> keyMapper, Function<Row, V> valueMapper) {
    Map<K, V> resultMap = new HashMap<>();
    for (Row row : rows) {
      resultMap.put(keyMapper.apply(row), valueMapper.apply(row));
    }
    return resultMap;
  }

  protected static PgConnectOptions getConnectOptions(
      PostgresConfig.ConnectOptions connectOptions) {
    return new PgConnectOptions()
        .setHost(connectOptions.getHost())
        .setPort(connectOptions.getPort())
        .setUser(connectOptions.getUser())
        .setPassword(connectOptions.getPassword())
        .setDatabase(connectOptions.getDatabase())
        .setCachePreparedStatements(connectOptions.getCachePreparedStatements());
  }

  protected static PoolOptions getPoolOptions(PostgresConfig.PoolOptions poolOptions) {
    return new PoolOptions()
        .setMaxSize(poolOptions.getMaxSize())
        .setMaxWaitQueueSize(poolOptions.getMaxWaitQueueSize());
  }
}
