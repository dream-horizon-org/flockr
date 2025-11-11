package com.ascend.flockr.io.request.source.stream;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = KafkaDataSource.class, name = "KAFKA"),
  @JsonSubTypes.Type(value = PulsarDataSource.class, name = "PULSAR"),
  @JsonSubTypes.Type(value = KinesisDataSource.class, name = "KINESIS")
})
public sealed interface StreamDataSource
    permits KafkaDataSource, PulsarDataSource, KinesisDataSource {

  StreamSourceType getType();
}
