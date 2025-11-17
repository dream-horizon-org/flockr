package com.ascend.flockr.client.postgres;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.SqlConnection;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.List;
import java.util.function.Function;

/**
 * Client interface for writing data to PostgreSQL database.
 *
 * <p>This client provides methods for:
 * <ul>
 *   <li>Executing write operations (INSERT, UPDATE, DELETE)</li>
 *   <li>Executing batch operations</li>
 *   <li>Managing database connections</li>
 *   <li>Executing operations within transactions</li>
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
public interface PostgresWriterClient {
  /**
   * Closes the database connection pool and releases all associated resources.
   *
   * @return a Completable that completes when the connection pool is closed
   */
  Completable close();

  /**
   * Gets a database connection from the connection pool.
   *
   * @return a Single emitting a SqlConnection that must be closed after use
   */
  Single<SqlConnection> getConnection();

  /**
   * Executes a prepared statement with the given parameters.
   *
   * @param connection the database connection to use
   * @param preparedQuery the SQL query with placeholders
   * @param tuple the parameters for the prepared statement
   * @return a Single emitting true if the statement executed successfully
   */
  Single<Boolean> execute(SqlConnection connection, String preparedQuery, Tuple tuple);

  /**
   * Executes a prepared INSERT statement and returns the generated ID.
   *
   * @param connection the database connection to use
   * @param preparedQuery the INSERT query with RETURNING clause
   * @param tuple the parameters for the prepared statement
   * @return a Single emitting the generated ID
   */
  Single<Long> executeAndGenerateId(SqlConnection connection, String preparedQuery, Tuple tuple);

  /**
   * Executes a prepared statement multiple times with different parameter sets (batch operation).
   *
   * @param connection the database connection to use
   * @param preparedQuery the SQL query with placeholders
   * @param tuples the list of parameter sets to execute
   * @return a Single emitting true if all statements executed successfully
   */
  Single<Boolean> executeMultiple(
      SqlConnection connection, String preparedQuery, List<Tuple> tuples);

  /**
   * Executes a function within a database transaction.
   *
   * <p>The transaction is automatically committed if the function completes successfully, or rolled
   * back if it fails or returns an empty Maybe.
   *
   * @param transactionalFunction the function to execute within the transaction
   * @param <T> the type of result returned by the function
   * @return a Maybe emitting the result of the transactional function, or empty if the transaction
   *     was rolled back
   */
  <T> Maybe<T> executeWithTransaction(Function<SqlConnection, Maybe<T>> transactionalFunction);
}
