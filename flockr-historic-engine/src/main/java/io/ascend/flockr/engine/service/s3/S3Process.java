package io.ascend.flockr.engine.service.s3;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.ascend.flockr.engine.modules.sink.Sink;
import io.ascend.flockr.engine.modules.source.Source;
import io.ascend.flockr.engine.service.BaseProcess;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Core processing service for the Flocker Historic Engine.
 *
 * <p>This class extends {@link BaseProcess} and provides S3-specific data processing functionality.
 * It orchestrates the data processing pipeline:
 *
 * <ol>
 *   <li>Reads data from the configured source (e.g., Athena)
 *   <li>Applies SQL queries if needed
 *   <li>Transforms data to standardized format (user_id, audience_key, action, expire_at)
 *   <li>Writes results to all configured sinks (S3, API, Kafka)
 * </ol>
 *
 * <p><b>Data Transformation:</b>
 *
 * <p>The service transforms query results into a standardized schema:
 *
 * <ul>
 *   <li><b>user_id</b>: Extracted from the first column of the query result
 *   <li><b>audience_key</b>: Set to the audience name (maps to audienceName from application
 *       arguments)
 *   <li><b>action</b>: Set to "append" or "remove"
 *   <li><b>expire_at</b>: Set to the expiration timestamp (epoch seconds)
 * </ul>
 *
 * @see BaseProcess
 * @see Source
 * @see Sink
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class S3Process extends BaseProcess {

  /**
   * Creates a new S3Process instance with the specified dependencies.
   *
   * <p>This constructor is used by Guice dependency injection to provide the Spark session, source,
   * and sinks.
   *
   * @param sparkSession The Spark session for data processing.
   * @param source Single data source to read from.
   * @param sinks List of data sinks to write to.
   */
  @Inject
  public S3Process(
      SparkSession sparkSession,
      @Named("source") Source<Dataset<Row>> source,
      @Named("sinks") List<Sink<String>> sinks) {
    super(sparkSession, source, sinks);
  }

  /**
   * Checks if a column name is a valid user ID column.
   *
   * <p>For S3Process, only "user_id" (case-insensitive) is considered valid.
   *
   * @param columnName The column name to check.
   * @return true if the column name is "user_id" (case-insensitive).
   */
  @Override
  protected boolean isValidUserIdColumn(String columnName) {
    if (columnName == null) {
      return false;
    }
    return "user_id".equalsIgnoreCase(columnName);
  }
}
