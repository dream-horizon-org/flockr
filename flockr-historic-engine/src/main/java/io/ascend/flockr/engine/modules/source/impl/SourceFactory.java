package io.ascend.flockr.engine.modules.source.impl;

import io.ascend.flockr.engine.config.AthenaConfig;
import io.ascend.flockr.engine.config.S3Config;
import io.ascend.flockr.engine.enums.SourceTypes;
import io.ascend.flockr.engine.modules.source.Source;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Factory class for creating Source instances based on source type.
 *
 * <p>This factory provides a centralized way to create Source implementations based on the source
 * type enum. It handles type checking and validation of configuration objects.
 *
 * <p><b>Supported Source Types:</b>
 *
 * <ul>
 *   <li><b>S3</b>: Creates S3SourceImpl (requires S3Config)
 *   <li><b>ATHENA</b>: Creates AthenaSourceImpl (requires AthenaConfig)
 *   <li><b>REDSHIFT</b>: Not yet implemented (throws UnsupportedOperationException)
 *   <li><b>KAFKA</b>: Not yet implemented (throws UnsupportedOperationException)
 * </ul>
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * SourceTypes type = SourceTypes.ATHENA;
 * AthenaConfig config = AthenaConfig.fromConfig(...);
 * Source<Dataset<Row>> source = SourceFactory.createSource(type, config, sparkSession);
 * }</pre>
 *
 * @see Source
 * @see SourceTypes
 * @author Shivam-Raghuwanshi
 */
public class SourceFactory {

  /**
   * Creates a Source instance based on the source type and configuration.
   *
   * <p>This method validates that the config object matches the expected type for the source type
   * and creates the appropriate Source implementation.
   *
   * @param sourceType The type of source to create.
   * @param config The configuration object (must match the source type).
   * @param sparkSession The Spark session for the source.
   * @return A new Source instance.
   * @throws IllegalArgumentException If config is null or doesn't match the expected type.
   * @throws UnsupportedOperationException If the source type is not yet implemented.
   */
  public static Source<Dataset<Row>> createSource(
      SourceTypes sourceType, Object config, SparkSession sparkSession) {
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null for source type: " + sourceType);
    }

    return switch (sourceType) {
      case S3 -> {
        if (!(config instanceof S3Config)) {
          throw new IllegalArgumentException(
              "S3 source requires S3Config, got: " + config.getClass().getName());
        }
        yield new S3SourceImpl((S3Config) config, sparkSession);
      }
      case ATHENA -> {
        if (!(config instanceof AthenaConfig)) {
          throw new IllegalArgumentException(
              "Athena source requires AthenaConfig, got: " + config.getClass().getName());
        }
        yield new AthenaSourceImpl((AthenaConfig) config, sparkSession);
      }
      case REDSHIFT -> throw new UnsupportedOperationException(
          "Redshift source implementation not yet available");
      case KAFKA -> throw new UnsupportedOperationException(
          "Kafka source implementation not yet available");
      default -> throw new IllegalArgumentException("Unsupported source type: " + sourceType);
    };
  }
}
