package io.ascend.flockr.engine.modules.source;

import io.ascend.flockr.engine.dto.UserIdRow;
import org.apache.spark.sql.Dataset;

/**
 * Interface for data source implementations.
 *
 * <p>This interface defines the contract for reading data from various sources (e.g., AWS Athena,
 * S3, Kafka) and transforming it into a standardized format with user IDs.
 *
 * <p><b>Type Safety:</b>
 *
 * <p>Sources return Dataset&lt;UserIdRow&gt; instead of raw Dataset&lt;Row&gt; to provide
 * compile-time type safety. This ensures that all sources produce data in the expected format with
 * a user_id field.
 *
 * <p><b>Responsibility:</b>
 *
 * <p>Source implementations are responsible for:
 *
 * <ul>
 *   <li>Connecting to the data source (S3, Athena, Kafka, etc.)
 *   <li>Executing queries if needed
 *   <li>Extracting user IDs from the source data
 *   <li>Returning a type-safe Dataset&lt;UserIdRow&gt;
 * </ul>
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * Source source = new AthenaSourceImpl(config, sparkSession, sqlQuery);
 * Dataset<UserIdRow> userIds = source.read();
 * }</pre>
 *
 * <p><b>Implementations:</b>
 *
 * <ul>
 *   <li>{@link io.ascend.flockr.engine.modules.source.impl.AthenaSourceImpl} - AWS Athena source
 * </ul>
 *
 * @author Shivam-Raghuwanshi
 */
public interface Source {

  /**
   * Reads data from the source and returns it as a type-safe Dataset of UserIdRow.
   *
   * <p>This method should:
   *
   * <ul>
   *   <li>Establish connection to the data source
   *   <li>Execute queries or read data as appropriate
   *   <li>Extract user IDs and validate the user_id column exists
   *   <li>Convert to Dataset&lt;UserIdRow&gt; using Spark Encoders
   *   <li>Handle errors and throw appropriate exceptions
   * </ul>
   *
   * <p>For Athena sources, this will execute the SQL query and extract user IDs from results. For
   * S3 sources, this will read files from the specified S3 path and extract user IDs.
   *
   * @return A type-safe Dataset of UserIdRow containing user IDs.
   * @throws Exception If an error occurs while reading from the source or if user_id column is
   *     missing.
   */
  Dataset<UserIdRow> read() throws Exception;
}
