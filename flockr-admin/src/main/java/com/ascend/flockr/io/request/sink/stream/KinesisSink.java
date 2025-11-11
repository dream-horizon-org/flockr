package com.ascend.flockr.io.request.sink.stream; // KinesisSink.java

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class KinesisSink implements StreamDataSink {
  @Builder.Default StreamSinkType type = StreamSinkType.KINESIS;

  @NotBlank String region;
  @NotBlank String streamName;

  // Optional client hints
  String awsProfile;
  String roleArn;
  String endpointOverride;
}
