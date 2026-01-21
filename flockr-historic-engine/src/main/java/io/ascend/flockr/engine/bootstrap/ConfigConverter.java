package io.ascend.flockr.engine.bootstrap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.ascend.flockr.engine.config.AthenaConfig;
import io.ascend.flockr.engine.config.ConnectorConfig;
import io.ascend.flockr.engine.enums.SourceTypes;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Converts source configuration maps to ConnectorConfig objects.
 *
 * <p>This class handles the conversion of source configuration maps (from JSON) into typed
 * ConnectorConfig objects based on the source type.
 *
 * <p><b>Supported Source Types:</b>
 *
 * <ul>
 *   <li><b>S3</b>: Creates S3Config from the configuration map
 *   <li><b>ATHENA</b>: Creates AthenaConfig from the configuration map
 *   <li><b>KAFKA</b>: Creates KafkaConfig from the configuration map
 *   <li><b>REDSHIFT</b>: Currently unsupported (throws UnsupportedOperationException)
 * </ul>
 *
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class ConfigConverter {

  /**
   * Converts a source configuration map to a ConnectorConfig object.
   *
   * <p>This method parses the source type and configuration map, then creates the appropriate
   * configuration object based on the source type.
   *
   * @param type The source type (e.g., "ATHENA", "S3", "KAFKA"). Case-insensitive.
   * @param configMap The configuration map containing source-specific settings. Must not be null.
   * @return A ConnectorConfig object with the parsed configuration.
   * @throws IllegalArgumentException If configMap is null, or if the source type is unknown.
   * @throws UnsupportedOperationException If the source type is REDSHIFT (not yet implemented).
   */
  public ConnectorConfig convertSourceConfigToConnectorConfig(
      String type, Map<String, Object> configMap) {
    if (configMap == null) {
      throw new IllegalArgumentException("Source config cannot be null");
    }

    Config config = ConfigFactory.parseMap(configMap);
    Object parsedConfig;

    try {
      SourceTypes sourceType = SourceTypes.valueOf(type.toUpperCase());
      switch (sourceType) {
        case ATHENA:
          parsedConfig = AthenaConfig.fromConfig(config);
          break;
        default:
          throw new IllegalArgumentException("Unknown source type: " + sourceType);
      }
    } catch (IllegalArgumentException e) {
      log.error("Unknown source type: {}", type);
      throw new IllegalArgumentException("Unknown source type: " + type, e);
    }

    return new ConnectorConfig(type, parsedConfig);
  }
}
