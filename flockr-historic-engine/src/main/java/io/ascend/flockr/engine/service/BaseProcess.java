package io.ascend.flockr.engine.service;

import static org.apache.spark.sql.functions.lit;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.ascend.flockr.engine.modules.sink.Sink;
import io.ascend.flockr.engine.modules.source.Source;
import io.ascend.flockr.engine.modules.source.impl.AthenaSourceImpl;
import io.ascend.flockr.engine.utils.RowJsonConverter;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Base class for data processing services in the Flocker Historic Engine.
 *
 * <p>This class provides common functionality for processing data from sources and writing to
 * sinks. It handles:
 *
 * <ol>
 *   <li>Reading data from configured sources
 *   <li>Applying SQL queries if needed
 *   <li>Transforming data to standardized format (user_id, audience_key, action, expire_at)
 *   <li>Writing results to all configured sinks
 * </ol>
 *
 * <p><b>Data Transformation:</b>
 *
 * <p>The service transforms query results into a standardized schema:
 *
 * <ul>
 *   <li><b>user_id</b>: Extracted from the first column of the query result
 *   <li><b>audience_key</b>: Set to the audience name
 *   <li><b>action</b>: Set to "append" or "remove"
 *   <li><b>expire_at</b>: Set to the expiration timestamp (epoch seconds)
 * </ul>
 *
 * <p><b>Subclass Responsibilities:</b>
 *
 * <p>Subclasses should implement {@link #normalizeUserIdColumn(String)} to provide custom user ID
 * column normalization logic.
 *
 * @see Source
 * @see Sink
 * @author Shivam-Raghuwanshi
 */
@Slf4j
@Data
public abstract class BaseProcess {

  /** Spark session for distributed data processing. */
  protected final SparkSession sparkSession;

  /** Single configured data source. */
  protected final Source<Dataset<Row>> source;

  /** List of configured data sinks. */
  protected final List<Sink<String>> sinks;

  /**
   * Creates a new BaseProcess instance with the specified dependencies.
   *
   * <p>This constructor is used by Guice dependency injection to provide the Spark session, source,
   * and sinks.
   *
   * @param sparkSession The Spark session for data processing.
   * @param source Single data source to read from.
   * @param sinks List of data sinks to write to.
   */
  @Inject
  public BaseProcess(
      SparkSession sparkSession,
      @Named("source") Source<Dataset<Row>> source,
      @Named("sinks") List<Sink<String>> sinks) {
    this.sparkSession = sparkSession;
    this.source = source;
    this.sinks = sinks;
  }

  /**
   * Checks if a column name is a valid user ID column.
   *
   * <p>Subclasses can override this method to provide custom validation logic.
   *
   * @param columnName The column name to check.
   * @return true if the column name is a valid user ID column.
   */
  protected boolean isValidUserIdColumn(String columnName) {
    if (columnName == null) {
      return false;
    }
    String lowerColumnName = columnName.toLowerCase();
    return "user_id".equals(lowerColumnName);
  }

  /**
   * Normalizes a user ID column name to "user_id".
   *
   * <p>Subclasses can override this method to provide custom normalization logic.
   *
   * @param columnName The column name to normalize.
   * @return true if the column should be renamed to "user_id", false otherwise.
   */
  protected boolean shouldNormalizeUserIdColumn(String columnName) {
    return !isValidUserIdColumn(columnName);
  }

  /**
   * Processes data with the given query and writes results to all configured sinks.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Reads data from the configured source
   *   <li>Applies SQL query if needed (for non-Athena sources)
   *   <li>Transforms data to standardized format
   *   <li>Writes to all configured sinks
   * </ol>
   *
   * @param sqlQuery The SQL query to execute (may be ignored for Athena sources).
   * @param audienceName The name of the audience/cohort.
   * @param action The action to perform ("append" or "remove").
   * @param expireAt The expiration timestamp in epoch seconds.
   * @throws IllegalStateException If source is not configured or data reading fails.
   * @throws RuntimeException If writing to any sink fails.
   */
  public void processWithQuery(String sqlQuery, String audienceName, String action, Long expireAt) {
    try {
      log.info("Starting processing with query: {}", sqlQuery);
      log.info("Audience Name: {}, Action: {}", audienceName, action);

      if (source == null) {
        throw new IllegalStateException("Source is not configured");
      }

      Dataset<Row> data = source.read();
      if (data == null) {
        throw new IllegalStateException("Failed to read data from source");
      }

      data.cache();
      long rowCount = data.count();
      log.info("Successfully read data from source. Total row count: {}", rowCount);

      Dataset<Row> queryResult = applyQuery(data, sqlQuery);

      queryResult.cache();
      long resultCount = queryResult.count();
      log.info("Query result row count: {}", resultCount);

      Dataset<Row> resultWithFields = transformData(queryResult, audienceName, action, expireAt);

      log.info(
          "Transformed data structure - columns: {}",
          java.util.Arrays.toString(resultWithFields.columns()));

      writeToSinks(resultWithFields);

    } catch (Exception e) {
      log.error("Error processing data with query", e);
      throw new RuntimeException("Failed to process data with query", e);
    }
  }

  /**
   * Applies SQL query to the dataset if needed.
   *
   * <p>For Athena sources, the query is already executed, so the data is returned directly. For
   * other sources, the SQL query is applied to the dataset.
   *
   * @param data The source dataset.
   * @param sqlQuery The SQL query to apply.
   * @return The query result dataset.
   */
  protected Dataset<Row> applyQuery(Dataset<Row> data, String sqlQuery) {
    if (source instanceof AthenaSourceImpl) {
      log.info("Athena source detected - query already executed on Athena, using results directly");
      return data;
    } else {
      data.createOrReplaceTempView("source_data");
      log.debug("Created temporary view 'source_data'");
      return sparkSession.sql(sqlQuery);
    }
  }

  /**
   * Transforms the query result to the standardized format.
   *
   * @param queryResult The query result dataset.
   * @param audienceName The audience name.
   * @param action The action type.
   * @param expireAt The expiration timestamp.
   * @return The transformed dataset.
   */
  protected Dataset<Row> transformData(
      Dataset<Row> queryResult, String audienceName, String action, Long expireAt) {
    String[] columns = queryResult.columns();
    if (columns.length == 0) {
      throw new IllegalStateException("Query result has no columns");
    }

    String firstColumnName = columns[0];
    log.debug("First column name: {}", firstColumnName);

    Dataset<Row> resultWithFields = queryResult;

    // Normalize user_id column
    if (shouldNormalizeUserIdColumn(firstColumnName)) {
      resultWithFields = resultWithFields.withColumnRenamed(firstColumnName, "user_id");
      log.debug("Renamed column '{}' to 'user_id'", firstColumnName);
    } else {
      // Normalize to user_id if it's a valid user ID column but not already user_id
      String lowerColumnName = firstColumnName.toLowerCase();
      if (!"user_id".equals(lowerColumnName)) {
        resultWithFields = resultWithFields.withColumnRenamed(firstColumnName, "user_id");
        log.debug("Normalized column '{}' to 'user_id'", firstColumnName);
      }
    }

    resultWithFields = resultWithFields.select("user_id");

    // Add audience_key
    if (audienceName != null && !audienceName.isEmpty()) {
      resultWithFields = resultWithFields.withColumn("audience_key", lit(audienceName));
      log.debug("Added audience_key field: {}", audienceName);
    } else {
      resultWithFields = resultWithFields.withColumn("audience_key", lit(""));
    }

    // Add action
    if (action != null && !action.isEmpty()) {
      resultWithFields = resultWithFields.withColumn("action", lit(action));
      log.debug("Added action field: {}", action);
    } else {
      resultWithFields = resultWithFields.withColumn("action", lit("append"));
    }

    // Add expire_at
    if (expireAt != null) {
      resultWithFields = resultWithFields.withColumn("expire_at", lit(expireAt));
      log.debug("Added expire_at field: {} (epoch timestamp)", expireAt);
    } else {
      resultWithFields = resultWithFields.withColumn("expire_at", lit(0L));
    }

    return resultWithFields.select("user_id", "audience_key", "action", "expire_at");
  }

  /**
   * Writes the dataset to all configured sinks.
   *
   * @param dataset The dataset to write.
   */
  protected void writeToSinks(Dataset<Row> dataset) {
    log.info("Writing results to {} sinks...", sinks.size());
    for (Sink<String> sink : sinks) {
      try {
        sink.writeDataset(dataset);
        log.info("Successfully wrote dataset to sink: {}", sink.getClass().getSimpleName());
      } catch (UnsupportedOperationException e) {
        log.warn("Sink does not support writeDataset, falling back to individual writes", e);
        writeRowsToSink(dataset, sink, sink.getClass().getSimpleName());
      } catch (Exception e) {
        log.error("Failed to write to sink: {}", sink.getClass().getSimpleName(), e);
        throw new RuntimeException("Failed to write to sink", e);
      }

      try {
        sink.flush();
      } catch (Exception e) {
        log.debug("Sink flush failed or not supported", e);
      }
    }
  }

  /**
   * Writes dataset rows to a sink that doesn't support writeDataset.
   *
   * <p>This is a fallback method that collects all rows to the driver and writes them individually.
   * This may cause memory issues with large datasets.
   *
   * <p><b>Warning:</b> This method uses {@link Dataset#collectAsList()} which brings all data to
   * the driver node. For large datasets, this can cause out-of-memory errors. Sinks should
   * implement {@link Sink#writeDataset(Dataset)} to avoid this issue.
   *
   * @param dataset The dataset to write.
   * @param sink The sink to write to.
   * @param sinkName The name of the sink (for logging).
   */
  protected void writeRowsToSink(Dataset<Row> dataset, Sink<String> sink, String sinkName) {
    log.warn(
        "Using collectAsList for {} - this may cause memory issues with large datasets", sinkName);
    List<Row> rows = dataset.collectAsList();
    log.info("Collected {} rows to send to {}", rows.size(), sinkName);

    for (Row row : rows) {
      try {
        String json = RowJsonConverter.convertRowToJson(row);
        sink.write(json);
      } catch (Exception e) {
        log.error("Error writing row to sink: {}", sinkName, e);
        throw new RuntimeException("Failed to write row to sink: " + sinkName, e);
      }
    }

    log.info("Successfully sent {} rows to {}", rows.size(), sinkName);
  }
}
