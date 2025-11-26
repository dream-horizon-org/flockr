package io.ascend.flockr.admin.client.postgres.impl;

import com.ascend.flockr.client.postgres.AbstractPostgresClient;
import com.ascend.flockr.client.postgres.PostgresWriterClient;
import com.ascend.flockr.config.PostgresConfig;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.rxjava3.sqlclient.SqlConnection;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.List;
import java.util.function.Function;

public class PostgresWriterClientImpl extends AbstractPostgresClient
    implements PostgresWriterClient {

  @Inject
  public PostgresWriterClientImpl(Vertx vertx, PostgresConfig postgresConfig) {
    super(vertx, postgresConfig.getWriterConfig());
  }

  @Override
  public Completable close() {
    return super.rxClose();
  }

  @Override
  public Single<SqlConnection> getConnection() {
    return getPostgresPool().getConnection();
  }

  @Override
  public Single<Boolean> execute(SqlConnection connection, String preparedQuery, Tuple tuple) {
    return rxExecute(connection, preparedQuery, tuple).map(rows -> rows.rowCount() > 0);
  }

  @Override
  public Single<Long> executeAndGenerateId(
      SqlConnection connection, String preparedQuery, Tuple tuple) {
    return rxExecute(connection, preparedQuery, tuple)
        .map(
            (RowSet<Row> res) -> {
              if (res.size() == 0) {
                throw new IllegalStateException(
                    "No rows returned from INSERT query. Ensure query includes RETURNING id");
              }
              Row row = res.iterator().next();
              return row.getLong("id");
            });
  }

  @Override
  public Single<Boolean> executeMultiple(
      SqlConnection connection, String preparedQuery, List<Tuple> tuples) {
    return rxExecute(connection, preparedQuery, tuples).map(rows -> rows.rowCount() > 0);
  }

  @Override
  public <T> Maybe<T> executeWithTransaction(
      Function<SqlConnection, Maybe<T>> transactionalFunction) {
    return super.rxWithTransaction(transactionalFunction);
  }
}
