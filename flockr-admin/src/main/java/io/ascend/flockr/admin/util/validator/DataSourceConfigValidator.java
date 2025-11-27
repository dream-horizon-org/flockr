package io.ascend.flockr.admin.util.validator;

import io.vertx.core.json.JsonObject;

/**
 * @deprecated Use {@link ConfigValidatorRegistry} with {@link ConfigValidator} implementations
 *     instead. This class is kept for backward compatibility but will be removed in a future
 *     version.
 */
@Deprecated
public final class DataSourceConfigValidator {

  private DataSourceConfigValidator() {}

  public static void validate(JsonObject config, String connectorType) {
    if (config == null) {
      throw new IllegalArgumentException("config is required");
    }
    if (connectorType == null) {
      throw new IllegalArgumentException("connectorType is required");
    }
    switch (connectorType.toUpperCase()) {
      case "ATHENA" -> validateAthena(config);
      case "KAFKA" -> validateKafka(config);
      default -> throw new IllegalArgumentException(
          "Unsupported data source type: " + connectorType);
    }
  }

  private static void validateAthena(JsonObject cfg) {
    // Minimum requirement for now: query string
    if (isBlank(cfg.getString("query"))) {
      throw new IllegalArgumentException("Athena config requires 'query'");
    }
  }

  private static void validateKafka(JsonObject cfg) {
    if (isBlank(cfg.getString("topic"))) {
      throw new IllegalArgumentException("Kafka config requires 'topic'");
    }
    if (isBlank(cfg.getString("bootstrapServersUrl"))) {
      throw new IllegalArgumentException("Kafka config requires 'bootstrapServersUrl'");
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}
