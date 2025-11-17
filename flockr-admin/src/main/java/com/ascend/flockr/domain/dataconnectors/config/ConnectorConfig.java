package com.ascend.flockr.domain.dataconnectors.config;

/**
 * Base interface for all connector configurations.
 * Implementations are parsed using JsonSubTypes based on connector type.
 */
public interface ConnectorConfig {
  /**
   * Returns the connector type identifier.
   * This should match the type field in data_connector_types table.
   *
   * @return the connector type (e.g., "ATHENA", "KAFKA", "S3_FOLDER")
   */
  String getConnectorType();
}

