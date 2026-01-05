package com.dream11.flocker.engine.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic connector configuration for sources and sinks.
 *
 * <p>This class represents a type-safe wrapper around connector configurations. It is used
 * internally to represent both source and sink configurations in a unified way. The actual
 * configuration object type depends on the connector type.
 *
 * <p><b>For Sources:</b>
 *
 * <ul>
 *   <li>Type: "ATHENA", "S3", "KAFKA", etc.
 *   <li>Config: AthenaConfig, S3Config, KafkaConfig, etc.
 * </ul>
 *
 * <p><b>For Sinks:</b>
 *
 * <ul>
 *   <li>Type: "S3", "API", "KAFKA", etc.
 *   <li>Config: S3Config, ApiConfig, KafkaProducerConfig, etc.
 * </ul>
 *
 * <p>This class uses a custom deserializer ({@link ConnectorConfigDeserializer}) to properly
 * deserialize JSON configurations into the appropriate config objects.
 *
 * @see ConnectorConfigDeserializer
 * @see SourceConfig
 * @author Shivam-Raghuwanshi
 */
@Data
@NoArgsConstructor
@JsonDeserialize(using = ConnectorConfigDeserializer.class)
public class ConnectorConfig {

  /**
   * The type of connector (source or sink type).
   *
   * <p>Examples: "ATHENA", "S3", "API", "KAFKA"
   *
   * @return The connector type string.
   */
  private String type;

  /**
   * The configuration object for this connector.
   *
   * <p>The actual type of this object depends on the connector type:
   *
   * <ul>
   *   <li>ATHENA → AthenaConfig
   *   <li>S3 → S3Config
   *   <li>API → ApiConfig
   *   <li>KAFKA → KafkaConfig or KafkaProducerConfig
   * </ul>
   *
   * @return The configuration object, or null if not set.
   */
  private Object config;

  /**
   * Creates a new ConnectorConfig with the specified type and configuration.
   *
   * @param type The connector type (e.g., "ATHENA", "S3", "API").
   * @param config The configuration object for this connector type.
   */
  public ConnectorConfig(String type, Object config) {
    this.type = type;
    this.config = config;
  }
}
