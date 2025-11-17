package com.ascend.flockr.client.postgres;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Client interface for reading data from PostgreSQL database.
 *
 * <p>This client provides methods for:
 *
 * <ul>
 *   <li>Executing queries and fetching results
 *   <li>Mapping database rows to domain objects
 *   <li>Fetching single rows or multiple rows
 *   <li>Fetching results as maps
 * </ul>
 *
 * <p>All methods support both plain SQL queries and prepared statements with parameters.
 *
 * @author Flockr Team
 * @since 1.0
 */
public interface PostgresReaderClient {
  /**
   * Closes the database connection and releases all associated resources.
   *
   * @return a Completable that completes when the connection is closed
   */
  Completable close();

  /**
   * Checks if the database connection is active.
   *
   * @return a Single emitting true if connected, false otherwise
   */
  Single<Boolean> isConnected();

  /**
   * Executes a query and fetches all results, mapping each row using the provided mapper.
   *
   * @param query the SQL query to execute
   * @param rowMapper function to map each row to a domain object
   * @param <T> the type of objects to return
   * @return a Single emitting a list of mapped objects
   */
  <T> Single<List<T>> fetchAll(String query, Function<Row, T> rowMapper);

  /**
   * Executes a prepared statement and fetches all results, mapping each row using the provided
   * mapper.
   *
   * @param preparedQuery the SQL query with placeholders
   * @param tuple the parameters for the prepared statement
   * @param rowMapper function to map each row to a domain object
   * @param <T> the type of objects to return
   * @return a Single emitting a list of mapped objects
   */
  <T> Single<List<T>> fetchAll(String preparedQuery, Tuple tuple, Function<Row, T> rowMapper);

  /**
   * Executes a prepared statement and fetches a single row, mapping it using the provided mapper.
   *
   * @param preparedQuery the SQL query with placeholders
   * @param tuple the parameters for the prepared statement
   * @param rowMapper function to map the row to a domain object
   * @param <T> the type of object to return
   * @return a Single emitting the mapped object, or an error if no row is found
   */
  <T> Single<T> fetchOne(String preparedQuery, Tuple tuple, Function<Row, T> rowMapper);

  /**
   * Executes a query and fetches results as a map, using key and value mappers.
   *
   * @param query the SQL query to execute
   * @param keyMapper function to extract the key from each row
   * @param valueMapper function to extract the value from each row
   * @param <K> the type of map keys
   * @param <V> the type of map values
   * @return a Single emitting a map of key-value pairs
   */
  <K, V> Single<Map<K, V>> fetchMap(
      String query, Function<Row, K> keyMapper, Function<Row, V> valueMapper);

  /**
   * Executes a prepared statement and fetches results as a map, using key and value mappers.
   *
   * @param preparedQuery the SQL query with placeholders
   * @param tuple the parameters for the prepared statement
   * @param keyMapper function to extract the key from each row
   * @param valueMapper function to extract the value from each row
   * @param <K> the type of map keys
   * @param <V> the type of map values
   * @return a Single emitting a map of key-value pairs
   */
  <K, V> Single<Map<K, V>> fetchMap(
      String preparedQuery, Tuple tuple, Function<Row, K> keyMapper, Function<Row, V> valueMapper);
}
