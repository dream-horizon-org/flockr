package io.ascend.flockr.admin.domain.dataconnectors.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for all source connector configurations. Uses JsonSubTypes for polymorphic
 * deserialization based on connectorType property.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "connectorType",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AthenaSourceConfig.class, name = "ATHENA"),
  @JsonSubTypes.Type(value = KafkaSourceConfig.class, name = "KAFKA")
})
public interface SourceConfig extends ConnectorConfig {}
