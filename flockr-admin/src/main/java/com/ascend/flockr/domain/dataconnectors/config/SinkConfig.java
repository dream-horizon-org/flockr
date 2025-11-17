package com.ascend.flockr.domain.dataconnectors.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for all sink connector configurations. Uses JsonSubTypes for polymorphic
 * deserialization based on connectorType property.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "connectorType",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = KafkaSinkConfig.class, name = "KAFKA"),
  @JsonSubTypes.Type(value = S3FolderSinkConfig.class, name = "S3_FOLDER")
})
public interface SinkConfig extends ConnectorConfig {}
