package com.ascend.flockr.io.request.sink.stream; // KafkaSink.java

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public final class KafkaSink implements StreamDataSink {
  @Builder.Default StreamSinkType type = StreamSinkType.KAFKA;

  @Singular("broker")
  List<String> brokers;

  @NotBlank String topic;
}
