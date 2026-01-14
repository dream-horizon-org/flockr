package io.ascend.flockr.engine.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.ascend.flockr.engine.config.ApiConfig;
import io.ascend.flockr.engine.config.AthenaConfig;
import io.ascend.flockr.engine.config.ConnectorConfig;
import io.ascend.flockr.engine.config.KafkaConfig;
import io.ascend.flockr.engine.config.S3Config;
import io.ascend.flockr.engine.enums.SourceTypes;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectorConfigParser {

  public static ConnectorConfig parseSourceConfig(String jsonConfig) {
    try {
      Config config = ConfigFactory.parseString(jsonConfig);
      return parseSourceConfigFromConfig(config);
    } catch (Exception e) {
      log.error("Failed to parse source configuration: {}", jsonConfig, e);
      throw new IllegalArgumentException("Invalid source configuration: " + e.getMessage(), e);
    }
  }

  public static List<ConnectorConfig> parseSourceConfigList(String jsonConfig) {
    try {
      List<ConnectorConfig> configs = new ArrayList<>();
      Config listConfig = ConfigFactory.parseString("items=" + jsonConfig);

      for (Config config : listConfig.getConfigList("items")) {
        configs.add(parseSourceConfigFromConfig(config));
      }
      return configs;
    } catch (Exception e) {
      log.error("Failed to parse source configuration list: {}", jsonConfig, e);
      throw new IllegalArgumentException("Invalid source configuration list: " + e.getMessage(), e);
    }
  }

  /**
   * Parses a single source configuration from a Config object.
   *
   * <p>This method extracts the source type and configuration, then creates the appropriate
   * ConnectorConfig object.
   *
   * @param config The Typesafe Config object containing source configuration.
   * @return A ConnectorConfig object with the parsed configuration.
   * @throws IllegalArgumentException If the config is missing required fields or has an invalid
   *     type.
   */
  private static ConnectorConfig parseSourceConfigFromConfig(Config config) {
    if (!config.hasPath("type")) {
      throw new IllegalArgumentException("Source configuration must have 'type' field");
    }

    String type = config.getString("type").toUpperCase();
    SourceTypes sourceType = SourceTypes.valueOf(type);

    Config sourceConfig =
        config.hasPath("config") ? config.getConfig("config") : ConfigFactory.empty();

    Object parsedConfig;
    switch (sourceType) {
      case S3:
        parsedConfig = S3Config.fromConfig(sourceConfig);
        break;
      case ATHENA:
        parsedConfig = AthenaConfig.fromConfig(sourceConfig);
        break;
      case KAFKA:
        parsedConfig = KafkaConfig.fromConfig(sourceConfig);
        break;
      case REDSHIFT:
        throw new UnsupportedOperationException("Redshift source not yet implemented");
      default:
        throw new IllegalArgumentException("Unknown source type: " + sourceType);
    }

    return new ConnectorConfig(type, parsedConfig);
  }

  public static List<ConnectorConfig> parseSinkConfigList(String jsonConfig) {
    try {
      List<ConnectorConfig> configs = new ArrayList<>();
      Config listConfig = ConfigFactory.parseString("items=" + jsonConfig);

      for (Config config : listConfig.getConfigList("items")) {
        configs.add(parseSinkConfig(config));
      }
      return configs;
    } catch (Exception e) {
      log.error("Failed to parse sink configuration list: {}", jsonConfig, e);
      throw new IllegalArgumentException("Invalid sink configuration list: " + e.getMessage(), e);
    }
  }

  private static ConnectorConfig parseSinkConfig(Config config) {
    if (!config.hasPath("type")) {
      throw new IllegalArgumentException("Sink configuration must have 'type' field");
    }

    String type = config.getString("type").toUpperCase();
    Config sinkConfig =
        config.hasPath("config") ? config.getConfig("config") : ConfigFactory.empty();

    if ("API".equals(type)) {
      ApiConfig apiConfig = ApiConfig.fromConfig(sinkConfig);
      return new ConnectorConfig(type, apiConfig);
    } else if ("S3".equals(type)) {
      return new ConnectorConfig(type, sinkConfig);
    } else {
      return new ConnectorConfig(type, sinkConfig);
    }
  }
}
