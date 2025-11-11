package com.ascend.flockr.io.request.sink.stream; // PulsarSink.java

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public final class PulsarSink implements StreamDataSink {
  @Builder.Default StreamSinkType type = StreamSinkType.PULSAR;

  @NotBlank String serviceUrl; // pulsar://...
  @NotBlank String topic; // persistent://tenant/ns/topic
}
