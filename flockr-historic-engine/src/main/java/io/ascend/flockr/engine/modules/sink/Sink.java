package io.ascend.flockr.engine.modules.sink;

import io.ascend.flockr.engine.dto.AudienceMetadata;
import io.ascend.flockr.engine.dto.UserIdRow;
import org.apache.spark.sql.Dataset;

/**
 * Interface for data sink implementations.
 *
 * <p>This interface defines the contract for writing user ID data along with audience metadata to
 * various destinations (e.g., S3, REST APIs, Kafka). Implementations should handle
 * destination-specific connection, authentication, and data writing logic.
 *
 * <p><b>Implementations:</b>
 *
 * <ul>
 *   <li>{@link io.ascend.flockr.engine.modules.sink.impl.S3SinkImpl} - S3 sink (Parquet/CSV)
 *   <li>{@link io.ascend.flockr.engine.modules.sink.impl.ApiSinkImpl} - REST API sink
 *   <li>{@link io.ascend.flockr.engine.modules.sink.impl.KafkaSinkImpl} - Kafka sink
 * </ul>
 *
 * @author Shivam-Raghuwanshi
 */
public interface Sink {
  /**
   * Writes user IDs with audience metadata to the sink.
   *
   * <p>This method writes a Dataset of user IDs along with audience metadata (name, action,
   * expiration) to the configured destination. Implementations should:
   *
   * @param userIds The Dataset of user IDs to write.
   * @param metadata The audience metadata (name, action, expiration).
   */
  void write(Dataset<UserIdRow> userIds, AudienceMetadata metadata);
}
