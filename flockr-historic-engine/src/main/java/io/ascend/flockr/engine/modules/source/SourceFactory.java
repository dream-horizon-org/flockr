package io.ascend.flockr.engine.modules.source;

import io.ascend.flockr.engine.config.AthenaConfig;
import io.ascend.flockr.engine.enums.SourceTypes;
import io.ascend.flockr.engine.modules.source.impl.AthenaSourceImpl;
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
 *   <li><b>ATHENA</b>: Creates AthenaSourceImpl (requires AthenaConfig)
 * </ul>
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * SourceTypes type = SourceTypes.ATHENA;
 * AthenaConfig config = AthenaConfig.fromConfig(...);
 * Source source = SourceFactory.createSource(type, config, sparkSession);
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
  public static Source createSource(
      SourceTypes sourceType, Object config, SparkSession sparkSession) {
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null for source type: " + sourceType);
    }

    switch (sourceType) {
      case ATHENA:
        if (!(config instanceof AthenaConfig)) {
          throw new IllegalArgumentException(
              "Athena source requires AthenaConfig, got: " + config.getClass().getName());
        }
        return new AthenaSourceImpl((AthenaConfig) config, sparkSession);
      default:
        throw new IllegalArgumentException("Unsupported source type: " + sourceType);
    }
  }
}
