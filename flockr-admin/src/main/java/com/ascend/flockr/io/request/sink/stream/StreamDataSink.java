package com.ascend.flockr.io.request.sink.stream; // StreamDataSink.java

import com.fasterxml.jackson.annotation.*;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = KafkaSink.class, name = "KAFKA"),
  @JsonSubTypes.Type(value = PulsarSink.class, name = "PULSAR"),
  @JsonSubTypes.Type(value = KinesisSink.class, name = "KINESIS")
})
public sealed interface StreamDataSink permits KafkaSink, PulsarSink, KinesisSink {
  StreamSinkType getType();
}
