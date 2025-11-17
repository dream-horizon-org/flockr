package com.ascend.flockr.util;

import com.ascend.flockr.domain.dataconnectors.config.ConnectorConfig;
import com.ascend.flockr.domain.dataconnectors.config.SinkConfig;
import com.ascend.flockr.domain.dataconnectors.config.SourceConfig;
import com.ascend.flockr.exception.ConfigParsingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for parsing JsonObject configs into typed POJO classes using JsonSubTypes.
 * Automatically detects the correct implementation based on field patterns.
 */
@Slf4j
@UtilityClass
public final class ConfigParser {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Parses JsonObject to SourceConfig implementation using JsonSubTypes. Automatically detects the
   * correct implementation (AthenaSourceConfig, KafkaSourceConfig, etc.) based on connectorType
   * property.
   *
   * @param configJson the JsonObject config from database
   * @return parsed SourceConfig implementation
   * @throws ConfigParsingException if parsing fails
   */
  public static SourceConfig parseSourceConfig(JsonObject configJson) {
    if (configJson == null) {
      throw new ConfigParsingException("SOURCE", "Config JsonObject cannot be null");
    }
    try {
      return objectMapper.readValue(configJson.encode(), new TypeReference<SourceConfig>() {});
    } catch (ConfigParsingException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to parse source config: {}", configJson, e);
      throw new ConfigParsingException(
          "SOURCE", "Failed to parse source config: " + e.getMessage(), e);
    }
  }

  /**
   * Parses JsonObject to SinkConfig implementation using JsonSubTypes. Automatically detects the
   * correct implementation (KafkaSinkConfig, S3FolderSinkConfig, etc.) based on connectorType
   * property.
   *
   * @param configJson the JsonObject config from database
   * @return parsed SinkConfig implementation
   * @throws ConfigParsingException if parsing fails
   */
  public static SinkConfig parseSinkConfig(JsonObject configJson) {
    if (configJson == null) {
      throw new ConfigParsingException("SINK", "Config JsonObject cannot be null");
    }
    try {
      return objectMapper.readValue(configJson.encode(), new TypeReference<SinkConfig>() {});
    } catch (ConfigParsingException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to parse sink config: {}", configJson, e);
      throw new ConfigParsingException("SINK", "Failed to parse sink config: " + e.getMessage(), e);
    }
  }

  /**
   * Parses JsonObject to ConnectorConfig implementation using JsonSubTypes. Automatically detects
   * whether it's a SourceConfig or SinkConfig based on connectorKind.
   *
   * @param configJson the JsonObject config from database
   * @param connectorKind the connector kind ("SOURCE" or "SINK")
   * @return parsed ConnectorConfig implementation
   * @throws ConfigParsingException if parsing fails
   */
  public static ConnectorConfig parseConfig(JsonObject configJson, String connectorKind) {
    if (configJson == null) {
      throw new ConfigParsingException(connectorKind, "Config JsonObject cannot be null");
    }
    if (connectorKind == null) {
      throw new ConfigParsingException("Connector kind cannot be null");
    }

    try {
      if ("SOURCE".equalsIgnoreCase(connectorKind)) {
        return parseSourceConfig(configJson);
      } else if ("SINK".equalsIgnoreCase(connectorKind)) {
        return parseSinkConfig(configJson);
      } else {
        throw new ConfigParsingException(
            connectorKind,
            "Invalid connector kind: " + connectorKind + ". Must be 'SOURCE' or 'SINK'");
      }
    } catch (ConfigParsingException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to parse config for kind {}: {}", connectorKind, configJson, e);
      throw new ConfigParsingException(
          connectorKind,
          String.format("Failed to parse %s config: %s", connectorKind, e.getMessage()),
          e);
    }
  }
}
