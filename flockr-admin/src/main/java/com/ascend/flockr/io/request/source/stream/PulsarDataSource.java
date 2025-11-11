package com.ascend.flockr.io.request.source.stream; // PulsarDataSource.java

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class PulsarDataSource implements StreamDataSource {
  @Builder.Default StreamSourceType type = StreamSourceType.PULSAR;

  /** e.g., "pulsar://broker:6650" or "pulsar+ssl://..." */
  @NotBlank String serviceUrl;

  /** Full topic name, e.g., "persistent://tenant/ns/topic" */
  @NotBlank String topic;
}
