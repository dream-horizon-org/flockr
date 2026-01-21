package io.ascend.flockr.admin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.typesafe.config.*;
import io.ascend.flockr.admin.constants.Constants;
import io.ascend.flockr.admin.domain.dataconnectors.config.SinkConfig;
import io.ascend.flockr.admin.domain.dataconnectors.config.SourceConfig;
import io.ascend.flockr.admin.exception.ConfigParsingException;
import io.vertx.core.json.JsonObject;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified utility class for application configuration and config parsing.
 *
 * <p>This utility provides comprehensive configuration management including:
 *
 * <ul>
 *   <li>Loading application configuration from HOCON files (TypeSafe Config)
 *   <li>Parsing JsonObject configs into typed POJO classes using JsonSubTypes
 *   <li>Environment-aware configuration loading (dev, staging, prod, etc.)
 *   <li>Type-safe configuration bean creation
 * </ul>
 *
 * <p><b>Configuration File Loading:</b>
 *
 * <p>The utility follows a fallback pattern: environment-specific config → default config
 *
 * <pre>{@code
 * // Loads from config/postgres/prod.conf -> config/postgres/default.conf
 * Config config = ConfigurationUtil.getConfigFromFile("config/postgres/%s");
 * }</pre>
 *
 * <p><b>Config Parsing:</b>
 *
 * <p>Automatically detects the correct implementation based on JsonSubTypes annotations:
 *
 * <pre>{@code
 * // Automatically detects AthenaSourceConfig, KafkaSourceConfig, etc.
 * SourceConfig source = ConfigurationUtil.parseSourceConfig(configJson);
 * }</pre>
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
@UtilityClass
public final class ConfigurationUtil {

  @Inject private static ObjectMapper objectMapper;

  // ==================== Application Configuration Methods ====================

  /**
   * Gets the current application environment from system properties.
   *
   * @return the environment name (defaults to "local" if not set)
   */
  private static String getAppEnvironment() {
    return System.getProperty(Constants.APP_ENV_KEY, Constants.DEFAULT_APP_ENV);
  }

  /**
   * Loads configuration from a HOCON file using environment-aware fallback.
   *
   * <p>The configFilePathFormat should contain a single %s placeholder for the environment name.
   * Example: "config/postgres/%s" will load "config/postgres/prod.conf" (or similar) based on the
   * current environment.
   *
   * <p>Falls back to default.conf if environment-specific config is not found.
   *
   * @param configFilePathFormat the config file path format with %s placeholder for environment
   * @return the loaded and resolved Config object
   */
  public static Config getConfigFromFile(@NonNull String configFilePathFormat) {
    ConfigFactory.invalidateCaches();
    String envFile = String.format(configFilePathFormat, getAppEnvironment());
    String defaultFile = String.format(configFilePathFormat, "default");
    Config config =
        ConfigFactory.load(envFile)
            .withFallback(
                ConfigFactory.load(
                    defaultFile,
                    ConfigParseOptions.defaults().setAllowMissing(true),
                    ConfigResolveOptions.defaults().setAllowUnresolved(true)))
            .resolve();
    log.debug("Loading config from file {} : {}", configFilePathFormat, config);
    return config;
  }

  /**
   * Loads configuration from a file and creates a typed configuration bean.
   *
   * <p>This method combines {@link #getConfigFromFile(String)} with ConfigBeanFactory to produce a
   * type-safe configuration object.
   *
   * @param <T> the type of configuration bean to create
   * @param configFilePathFormat the config file path format with %s placeholder
   * @param clazz the configuration class to instantiate
   * @return a typed configuration bean populated from the config file
   */
  public static <T> T getTypedConfigFromFile(@NonNull String configFilePathFormat, Class<T> clazz) {
    Config config = getConfigFromFile(configFilePathFormat);
    T typedConfig = ConfigBeanFactory.create(config, clazz);
    log.debug("Loaded Config: {}", typedConfig);
    return typedConfig;
  }

  // ==================== Config Parsing Methods ====================

  /**
   * Parses JsonObject to SourceConfig implementation using JsonSubTypes.
   *
   * <p>Automatically detects the correct implementation (AthenaSourceConfig, KafkaSourceConfig,
   * etc.) based on the connectorType property in the JSON.
   *
   * @param configJson the JsonObject config from database
   * @return parsed SourceConfig implementation
   * @throws ConfigParsingException if parsing fails or config is null
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
   * Parses JsonObject to SinkConfig implementation using JsonSubTypes.
   *
   * <p>Automatically detects the correct implementation (KafkaSinkConfig, S3FolderSinkConfig, etc.)
   * based on the connectorType property in the JSON.
   *
   * @param configJson the JsonObject config from database
   * @return parsed SinkConfig implementation
   * @throws ConfigParsingException if parsing fails or config is null
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
   * <p>This method provides an additional type-safety check by validating that the parsed config
   * matches the expected type.
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
