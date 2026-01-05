package com.dream11.flocker.engine.modules.source;

/**
 * Interface for data source implementations.
 *
 * <p>This interface defines the contract for reading data from various sources (e.g., AWS Athena,
 * S3, Kafka). Implementations should handle source-specific connection, authentication, and data
 * retrieval logic.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * Source<Dataset<Row>> source = new AthenaSourceImpl(config, sparkSession);
 * Dataset<Row> data = source.read();
 * }</pre>
 *
 * <p><b>Implementations:</b>
 *
 * <ul>
 *   <li>{@link com.dream11.flocker.engine.modules.source.impl.AthenaSourceImpl} - AWS Athena source
 *   <li>{@link com.dream11.flocker.engine.modules.source.impl.S3SourceImpl} - S3 source
 * </ul>
 *
 * @param <T> The type of data returned by this source (typically Dataset<Row> for Spark).
 * @author Shivam-Raghuwanshi
 */
public interface Source<T> {

  /**
   * Reads data from the source.
   *
   * <p>This method should:
   *
   * <ul>
   *   <li>Establish connection to the data source
   *   <li>Execute queries or read data as appropriate
   *   <li>Return the data in the expected format
   *   <li>Handle errors and throw appropriate exceptions
   * </ul>
   *
   * <p>For Athena sources, this will execute the SQL query and return results. For S3 sources, this
   * will read files from the specified S3 path.
   *
   * @return The data read from the source.
   * @throws Exception If an error occurs while reading from the source.
   */
  T read() throws Exception;
}
