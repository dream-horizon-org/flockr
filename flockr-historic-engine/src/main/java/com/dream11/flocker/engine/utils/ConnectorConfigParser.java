package com.dream11.flocker.engine.utils;

import com.dream11.flocker.engine.config.ApiConfig;
import com.dream11.flocker.engine.config.AthenaConfig;
import com.dream11.flocker.engine.config.ConnectorConfig;
import com.dream11.flocker.engine.config.KafkaConfig;
import com.dream11.flocker.engine.config.S3Config;
import com.dream11.flocker.engine.enums.SourceTypes;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectorConfigParser {

  public static ConnectorConfig parseSourceConfig(String jsonConfig) {
    try {
      Config config = ConfigFactory.parseString(jsonConfig);

      if (!config.hasPath("type")) {
        throw new IllegalArgumentException("Source configuration must have 'type' field");
      }

      String type = config.getString("type").toUpperCase();
      SourceTypes sourceType = SourceTypes.valueOf(type);

      Config sourceConfig =
          config.hasPath("config") ? config.getConfig("config") : ConfigFactory.empty();

      Object parsedConfig =
          switch (sourceType) {
            case S3 -> S3Config.fromConfig(sourceConfig);
            case ATHENA -> AthenaConfig.fromConfig(sourceConfig);
            case KAFKA -> KafkaConfig.fromConfig(sourceConfig);
            case REDSHIFT -> throw new UnsupportedOperationException(
                "Redshift source not yet implemented");
          };

      return new ConnectorConfig(type, parsedConfig);
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
        if (!config.hasPath("type")) {
          throw new IllegalArgumentException("Source configuration must have 'type' field");
        }

        String type = config.getString("type").toUpperCase();
        SourceTypes sourceType = SourceTypes.valueOf(type);

        Config sourceConfig =
            config.hasPath("config") ? config.getConfig("config") : ConfigFactory.empty();

        Object parsedConfig =
            switch (sourceType) {
              case S3 -> S3Config.fromConfig(sourceConfig);
              case ATHENA -> AthenaConfig.fromConfig(sourceConfig);
              case KAFKA -> KafkaConfig.fromConfig(sourceConfig);
              case REDSHIFT -> throw new UnsupportedOperationException(
                  "Redshift source not yet implemented");
            };

        configs.add(new ConnectorConfig(type, parsedConfig));
      }
      return configs;
    } catch (Exception e) {
      log.error("Failed to parse source configuration list: {}", jsonConfig, e);
      throw new IllegalArgumentException("Invalid source configuration list: " + e.getMessage(), e);
    }
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
