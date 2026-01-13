package io.ascend.flockr.engine.service.audience;

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
 * Core processing service for audience/cohort management in the Flocker Historic Engine.
 *
 * <p>This class extends {@link BaseProcess} and provides audience-specific data processing
 * functionality. It orchestrates the data processing pipeline:
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
 *   <li><b>user_id</b>: Extracted from the first column of the query result. Accepts column names:
 *       userId, userid, or user_id (case-insensitive)
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
public class AudienceProcess extends BaseProcess {

  /**
   * Creates a new AudienceProcess instance with the specified dependencies.
   *
   * <p>This constructor is used by Guice dependency injection to provide the Spark session, source,
   * and sinks.
   *
   * @param sparkSession The Spark session for data processing.
   * @param source Single data source to read from.
   * @param sinks List of data sinks to write to.
   */
  @Inject
  public AudienceProcess(
      SparkSession sparkSession,
      @Named("source") Source<Dataset<Row>> source,
      @Named("sinks") List<Sink<String>> sinks) {
    super(sparkSession, source, sinks);
  }

  /**
   * Checks if a column name is a valid user ID column (userId, userid, or user_id,
   * case-insensitive).
   *
   * <p>For AudienceProcess, multiple variations of user ID column names are accepted:
   *
   * <ul>
   *   <li>user_id (case-insensitive)
   *   <li>userid (case-insensitive)
   *   <li>userId (case-insensitive)
   * </ul>
   *
   * @param columnName The column name to check.
   * @return true if the column name matches any of the valid user ID column names.
   */
  @Override
  protected boolean isValidUserIdColumn(String columnName) {
    if (columnName == null) {
      return false;
    }
    String lowerColumnName = columnName.toLowerCase();
    return "user_id".equals(lowerColumnName)
        || "userid".equals(lowerColumnName)
        || "userid".equals(lowerColumnName.replace("_", ""));
  }
}
