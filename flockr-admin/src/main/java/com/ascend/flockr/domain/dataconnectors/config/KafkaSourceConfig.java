package com.ascend.flockr.domain.dataconnectors.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO for Kafka source connector configuration.
 * Used to parse JsonObject config retrieved from database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaSourceConfig implements SourceConfig {

  /** Required: Kafka topic name. */
  @JsonProperty("topic")
  private String topic;

  /** Required: Bootstrap servers URL (e.g., "localhost:9092" or "host1:9092,host2:9092"). */
  @JsonProperty("bootstrapServersUrl")
  private String bootstrapServersUrl;

  /** Connector type identifier for JsonSubTypes deserialization. */
  @JsonProperty("connectorType")
  @Builder.Default
  private String connectorType = "KAFKA";

  @Override
  public String getConnectorType() {
    return connectorType;
  }
}

