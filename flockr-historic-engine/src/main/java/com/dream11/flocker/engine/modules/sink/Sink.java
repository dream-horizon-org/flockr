package com.dream11.flocker.engine.modules.sink;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * Interface for data sink implementations.
 * 
 * <p>This interface defines the contract for writing data to various destinations
 * (e.g., S3, REST APIs, Kafka). Implementations should handle destination-specific
 * connection, authentication, and data writing logic.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * Sink<String> sink = new ApiSinkImpl(config, audienceName, action, expireAt);
 * sink.writeDataset(dataset);  // Preferred method for Spark datasets
 * // or
 * sink.write(jsonString);      // For individual records
 * sink.flush();                 // Ensure all data is written
 * }</pre>
 * 
 * <p><b>Implementations:</b>
 * <ul>
 *   <li>{@link com.dream11.flocker.engine.modules.sink.impl.S3SinkImpl} - S3 sink (Parquet/CSV)</li>
 *   <li>{@link com.dream11.flocker.engine.modules.sink.impl.ApiSinkImpl} - REST API sink</li>
 *   <li>{@link com.dream11.flocker.engine.modules.sink.impl.KafkaSinkImpl} - Kafka sink</li>
 * </ul>
 * 
 * @param <T> The type of data written by this sink (typically String for JSON).
 * @author Shivam-Raghuwanshi
 */
public interface Sink<T> {

    /**
     * Writes a single data item to the sink.
     * 
     * <p>This method is used for writing individual records (e.g., JSON strings).
     * For Spark datasets, prefer using {@link #writeDataset(Dataset)} for better
     * performance and distributed processing.
     * 
     * @param data The data item to write.
     * @throws Exception If an error occurs while writing to the sink.
     */
    void write(T data) throws Exception;

    /**
     * Writes a Spark Dataset to the sink.
     * 
     * <p>This is the preferred method for writing Spark datasets as it allows
     * for distributed processing and better performance. Implementations should
     * override this method to provide dataset-level writing capabilities.
     * 
     * <p>If not overridden, this method throws UnsupportedOperationException,
     * and the engine will fall back to collecting rows and calling {@link #write(Object)}
     * for each row (which may cause memory issues with large datasets).
     * 
     * @param dataset The Spark Dataset to write.
     * @throws Exception If an error occurs while writing to the sink.
     * @throws UnsupportedOperationException If this sink doesn't support dataset writing.
     */
    default void writeDataset(Dataset<Row> dataset) throws Exception {
        throw new UnsupportedOperationException("writeDataset not supported by this sink implementation");
    }

    /**
     * Flushes any buffered data to the sink.
     * 
     * <p>This method ensures that all buffered data is written to the destination.
     * It should be called after all write operations are complete to ensure data
     * integrity. Implementations may override this method to provide flush functionality.
     * 
     * <p>Default implementation does nothing (no-op), which is appropriate for
     * sinks that don't buffer data.
     * 
     * @throws Exception If an error occurs while flushing.
     */
    default void flush() throws Exception {}

}
