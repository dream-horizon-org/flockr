package com.ascend.flockr.io.request.source.stream;

// KafkaDataSource.java

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class KafkaDataSource implements StreamDataSource {

  @Builder.Default StreamSourceType type = StreamSourceType.KAFKA;

  @NotEmpty String bootstrapServersUrl;

  @NotBlank String topic;
}
