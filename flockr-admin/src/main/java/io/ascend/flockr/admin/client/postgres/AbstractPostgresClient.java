package io.ascend.flockr.admin.client.postgres;

import io.ascend.flockr.admin.config.PostgresConfig;
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

/**
 * Abstract base class for PostgreSQL database clients.
 *
 * <p>This class provides common functionality for PostgreSQL database operations including:
 *
 * <ul>
 *   <li>Connection pool management
 *   <li>Query execution with retry support
 *   <li>Transaction management
 *   <li>Result set mapping utilities
 * </ul>
 *
 * <p>Subclasses should implement specific read or write client functionality.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public abstract class AbstractPostgresClient {

  /** The PostgreSQL connection pool. */
  @Getter private final PgPool postgresPool;

  /** Number of retry attempts for failed queries. */
  private final Integer retryCount;

  /**
   * Constructs a new PostgreSQL client with the given configuration.
   *
   * @param vertx the Vert.x instance
   * @param postgresBaseConfig the PostgreSQL configuration
   */
  protected AbstractPostgresClient(Vertx vertx, PostgresConfig.BaseConfig postgresBaseConfig) {
    this.postgresPool =
        PgPool.pool(
            vertx,
            getConnectOptions(postgresBaseConfig.getConnectOptions()),
            getPoolOptions(postgresBaseConfig.getPoolOptions()));
    this.retryCount = postgresBaseConfig.getRetryCount();
  }

  /**
   * Closes the connection pool and releases all resources.
   *
   * @return a Completable that completes when the pool is closed
   */
  public Completable rxClose() {
    return this.postgresPool.rxClose();
  }

  /**
   * Executes a raw SQL query.
   *
   * @param query the SQL query to execute
   * @return a Single emitting the result set
   */
  protected Single<RowSet<Row>> rxExecute(String query) {
    return this.postgresPool.query(query).rxExecute().retry(this.retryCount);
  }

  /**
   * Executes a prepared statement with parameters.
   *
   * @param preparedQuery the SQL query with placeholders
   * @param tuple the parameters for the prepared statement
   * @return a Single emitting the result set
   */
  protected Single<RowSet<Row>> rxExecute(String preparedQuery, Tuple tuple) {
    return this.postgresPool.preparedQuery(preparedQuery).rxExecute(tuple).retry(this.retryCount);
  }

  /**
   * Executes a prepared statement on an existing connection.
   *
   * @param connection the database connection to use
   * @param preparedQuery the SQL query with placeholders
   * @param tuple the parameters for the prepared statement
   * @return a Single emitting the result set
   */
  protected Single<RowSet<Row>> rxExecute(
      SqlConnection connection, String preparedQuery, Tuple tuple) {
    return connection.preparedQuery(preparedQuery).rxExecute(tuple);
  }

  /**
   * Executes a batch prepared statement on an existing connection.
   *
   * @param connection the database connection to use
   * @param preparedQuery the SQL query with placeholders
   * @param tuples the list of parameter sets for batch execution
   * @return a Single emitting the result set
   */
  protected Single<RowSet<Row>> rxExecute(
      SqlConnection connection, String preparedQuery, List<Tuple> tuples) {
    return connection.preparedQuery(preparedQuery).rxExecuteBatch(tuples);
  }

  /**
   * Executes a function within a database transaction.
   *
   * @param transactionalFunction the function to execute within the transaction
   * @param <T> the type of result returned by the function
   * @return a Maybe emitting the result, or empty if the transaction was rolled back
   */
  protected <T> Maybe<T> rxWithTransaction(
      Function<SqlConnection, Maybe<T>> transactionalFunction) {
    return this.postgresPool.rxWithTransaction(transactionalFunction);
  }

  /**
   * Converts a result set to a list using the provided row mapper.
   *
   * @param rows the result set to convert
   * @param rowMapper function to map each row to a domain object
   * @param <T> the type of objects in the list
   * @return a list of mapped objects
   */
  protected static <T> List<T> toList(RowSet<Row> rows, Function<Row, T> rowMapper) {
    List<T> resultList = new ArrayList<>();
    for (Row row : rows) {
      resultList.add(rowMapper.apply(row));
    }
    return resultList;
  }

  /**
   * Converts a result set to a map using the provided key and value mappers.
   *
   * @param rows the result set to convert
   * @param keyMapper function to extract the key from each row
   * @param valueMapper function to extract the value from each row
   * @param <K> the type of map keys
   * @param <V> the type of map values
   * @return a map of key-value pairs
   */
  protected static <K, V> Map<K, V> toMap(
      RowSet<Row> rows, Function<Row, K> keyMapper, Function<Row, V> valueMapper) {
    Map<K, V> resultMap = new HashMap<>();
    for (Row row : rows) {
      resultMap.put(keyMapper.apply(row), valueMapper.apply(row));
    }
    return resultMap;
  }

  /**
   * Converts configuration to PgConnectOptions.
   *
   * @param connectOptions the connection configuration
   * @return configured PgConnectOptions
   */
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

  /**
   * Converts configuration to PoolOptions.
   *
   * @param poolOptions the pool configuration
   * @return configured PoolOptions
   */
  protected static PoolOptions getPoolOptions(PostgresConfig.PoolOptions poolOptions) {
    return new PoolOptions()
        .setMaxSize(poolOptions.getMaxSize())
        .setMaxWaitQueueSize(poolOptions.getMaxWaitQueueSize());
  }
}
