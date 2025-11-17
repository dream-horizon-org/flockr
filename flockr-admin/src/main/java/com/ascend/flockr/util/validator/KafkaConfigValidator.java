package com.ascend.flockr.util.validator;

import com.ascend.flockr.exception.ConfigValidationException;
import com.ascend.flockr.util.ConfigValidator;
import com.google.inject.Singleton;
import io.vertx.core.json.JsonObject;

/** Validator for Kafka connector configurations. Handles both SOURCE and SINK configurations. */
@Singleton
public class KafkaConfigValidator implements ConfigValidator {

  @Override
  public void validate(JsonObject config) {
    if (config == null) {
      throw new ConfigValidationException(
          getConnectorType(), getConnectorKind(), "Kafka config is required");
    }

    // Required fields for both source and sink
    if (isBlank(config.getString("topic"))) {
      throw new ConfigValidationException(
          getConnectorType(), getConnectorKind(), "Kafka config requires 'topic' field");
    }

    if (isBlank(config.getString("bootstrapServersUrl"))) {
      throw new ConfigValidationException(
          getConnectorType(),
          getConnectorKind(),
          "Kafka config requires 'bootstrapServersUrl' field");
    }

    // Validate bootstrapServersUrl format: host:port,host:port,...
    String bootstrapServersUrl = config.getString("bootstrapServersUrl");
    if (bootstrapServersUrl != null) {
      String[] servers = bootstrapServersUrl.split(",");
      for (String server : servers) {
        String trimmed = server.trim();
        if (!trimmed.matches("^[^:]+:[0-9]+$")) {
          throw new ConfigValidationException(
              getConnectorType(),
              getConnectorKind(),
              String.format(
                  "Kafka config 'bootstrapServersUrl' contains invalid server format: %s. "
                      + "Expected format: host:port (e.g., localhost:9092)",
                  trimmed));
        }
      }
    }
  }

  @Override
  public String getConnectorType() {
    return "KAFKA";
  }

  @Override
  public String getConnectorKind() {
    return null; // Kafka validator applies to both SOURCE and SINK
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}
