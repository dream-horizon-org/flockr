package com.ascend.flockr.util;

import com.ascend.flockr.exception.ConfigValidationException;
import io.vertx.core.json.JsonObject;

/**
 * Interface for validating connector-specific configuration JsonObjects. Implementations should
 * validate configurations for specific connector types.
 */
public interface ConfigValidator {

  /**
   * Validates the given configuration JsonObject.
   *
   * @param config the configuration to validate
   * @throws ConfigValidationException if the configuration is invalid
   */
  void validate(JsonObject config);

  /**
   * Returns the connector type this validator handles. This should match the type field in
   * data_connector_types table (e.g., "ATHENA", "KAFKA").
   *
   * @return the connector type in uppercase
   */
  String getConnectorType();

  /**
   * Returns the kind of connector this validator handles. This should match the kind field in
   * data_connector_types table (e.g., "SOURCE", "SINK"). If null, the validator applies to both
   * SOURCE and SINK.
   *
   * @return the connector kind in uppercase, or null if applicable to both
   */
  default String getConnectorKind() {
    return null; // null means applicable to both SOURCE and SINK
  }
}
