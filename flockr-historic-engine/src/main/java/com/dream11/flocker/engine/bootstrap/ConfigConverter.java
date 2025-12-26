package com.dream11.flocker.engine.bootstrap;

import com.dream11.flocker.engine.config.AthenaConfig;
import com.dream11.flocker.engine.config.ConnectorConfig;
import com.dream11.flocker.engine.config.KafkaConfig;
import com.dream11.flocker.engine.config.S3Config;
import com.dream11.flocker.engine.enums.SourceTypes;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Converts source configuration maps to ConnectorConfig objects.
 * 
 * <p>This class handles the conversion of source configuration maps (from JSON)
 * into typed ConnectorConfig objects based on the source type.
 * 
 * <p><b>Supported Source Types:</b>
 * <ul>
 *   <li><b>S3</b>: Creates S3Config from the configuration map</li>
 *   <li><b>ATHENA</b>: Creates AthenaConfig from the configuration map</li>
 *   <li><b>KAFKA</b>: Creates KafkaConfig from the configuration map</li>
 *   <li><b>REDSHIFT</b>: Currently unsupported (throws UnsupportedOperationException)</li>
 * </ul>
 * 
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class ConfigConverter {

    /**
     * Converts a source configuration map to a ConnectorConfig object.
     *
     * <p>This method parses the source type and configuration map, then creates
     * the appropriate configuration object based on the source type.
     *
     * @param type The source type (e.g., "ATHENA", "S3", "KAFKA"). Case-insensitive.
     * @param configMap The configuration map containing source-specific settings.
     *                  Must not be null.
     * @return A ConnectorConfig object with the parsed configuration.
     * @throws IllegalArgumentException If configMap is null, or if the source type is unknown.
     * @throws UnsupportedOperationException If the source type is REDSHIFT (not yet implemented).
     */
    public ConnectorConfig convertSourceConfigToConnectorConfig(String type, Map<String, Object> configMap) {
        if (configMap == null) {
            throw new IllegalArgumentException("Source config cannot be null");
        }

        Config config = ConfigFactory.parseMap(configMap);
        Object parsedConfig;

        try {
            SourceTypes sourceType = SourceTypes.valueOf(type.toUpperCase());
            parsedConfig = switch (sourceType) {
                case S3 -> S3Config.fromConfig(config);
                case ATHENA -> AthenaConfig.fromConfig(config);
                case KAFKA -> KafkaConfig.fromConfig(config);
                case REDSHIFT -> throw new UnsupportedOperationException("Redshift source not yet implemented");
            };
        } catch (IllegalArgumentException e) {
            log.error("Unknown source type: {}", type);
            throw new IllegalArgumentException("Unknown source type: " + type, e);
        }

        return new ConnectorConfig(type, parsedConfig);
    }
}

