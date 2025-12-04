package io.ascend.flockr.admin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.ascend.flockr.admin.domain.dataconnectors.config.SinkConfig;
import io.ascend.flockr.admin.domain.dataconnectors.config.SourceConfig;
import io.ascend.flockr.admin.exception.ConfigParsingException;
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

  @Inject private static ObjectMapper objectMapper;

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
   * Parses JsonObject to a specific SinkConfig type with compile-time type safety.
   *
   * @param <T> the expected SinkConfig type
   * @param configJson the JsonObject config from database
   * @param expectedType the expected config class
   * @return parsed config of the expected type
   * @throws ConfigParsingException if parsing fails or type doesn't match
   */
  public static <T extends SinkConfig> T parseSinkConfig(
      JsonObject configJson, Class<T> expectedType) {
    SinkConfig config = parseSinkConfig(configJson);
    if (!expectedType.isInstance(config)) {
      throw new ConfigParsingException(
          "SINK",
          String.format(
              "Expected config type %s but got %s",
              expectedType.getSimpleName(), config.getClass().getSimpleName()));
    }
    return expectedType.cast(config);
  }
}
