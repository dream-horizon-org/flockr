package io.ascend.flockr.admin.util.validator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ascend.flockr.admin.exception.ConfigValidationException;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry that manages and delegates to appropriate ConfigValidator implementations. Uses a
 * composite key of connector type and kind to route validation requests.
 */
@Slf4j
@Singleton
@Deprecated
public class ConfigValidatorRegistry {

  private final Map<String, ConfigValidator> validators = new ConcurrentHashMap<>();

  @Inject
  public ConfigValidatorRegistry(Set<ConfigValidator> validators) {
    for (ConfigValidator validator : validators) {
      String key = buildKey(validator.getConnectorType(), validator.getConnectorKind());
      this.validators.put(key, validator);
      log.info(
          "Registered config validator for connector type: {}, kind: {}",
          validator.getConnectorType(),
          validator.getConnectorKind() != null ? validator.getConnectorKind() : "BOTH");
    }
  }

  /**
   * Validates the given configuration for the specified connector type and kind.
   *
   * @param config the configuration to validate
   * @param connectorType the connector type (e.g., "ATHENA", "KAFKA")
   * @param connectorKind the connector kind (e.g., "SOURCE", "SINK")
   * @throws ConfigValidationException if no validator is found or if validation fails
   */
  public void validate(JsonObject config, String connectorType, String connectorKind) {
    if (config == null) {
      throw new ConfigValidationException("config is required");
    }
    if (connectorType == null) {
      throw new ConfigValidationException("connectorType is required");
    }
    if (connectorKind == null) {
      throw new ConfigValidationException("connectorKind is required");
    }

    String normalizedType = connectorType.toUpperCase();
    String normalizedKind = connectorKind.toUpperCase();

    // First try to find a validator specific to this kind
    String specificKey = buildKey(normalizedType, normalizedKind);
    ConfigValidator validator = validators.get(specificKey);

    // If not found, try to find a validator that applies to both kinds
    if (validator == null) {
      String genericKey = buildKey(normalizedType, null);
      validator = validators.get(genericKey);
    }

    if (validator == null) {
      throw new ConfigValidationException(
          "CONFIG_VALIDATOR_NOT_FOUND",
          String.format(
              "No validator found for connector type: %s, kind: %s",
              normalizedType, normalizedKind));
    }

    try {
      validator.validate(config);
    } catch (ConfigValidationException e) {
      throw e; // Re-throw validation errors as-is
    } catch (Exception e) {
      throw new ConfigValidationException(
          normalizedType,
          normalizedKind,
          String.format(
              "Validation failed for connector type: %s, kind: %s. Error: %s",
              normalizedType, normalizedKind, e.getMessage()),
          e);
    }
  }

  /**
   * Builds a composite key from connector type and kind.
   *
   * @param connectorType the connector type
   * @param connectorKind the connector kind (can be null for generic validators)
   * @return the composite key
   */
  private String buildKey(String connectorType, String connectorKind) {
    if (connectorKind == null) {
      return connectorType + "::*";
    }
    return connectorType + "::" + connectorKind;
  }

  /**
   * Returns all registered connector types.
   *
   * @return set of connector types
   */
  public Set<String> getRegisteredConnectorTypes() {
    return validators.values().stream()
        .map(ConfigValidator::getConnectorType)
        .collect(Collectors.toSet());
  }
}
