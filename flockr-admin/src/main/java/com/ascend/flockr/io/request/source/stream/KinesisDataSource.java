package com.ascend.flockr.io.request.source.stream;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class KinesisDataSource implements StreamDataSource {

  @Builder.Default StreamSourceType type = StreamSourceType.KINESIS;

  /** AWS region, e.g., "us-east-1" */
  @NotBlank String region;

  /** Kinesis stream name */
  @NotBlank String streamName;

  /** Optional application name if you use KCL (used for leases/checkpoints in DynamoDB) */
  String applicationName;

  /** Optional profile or role hint if you resolve credentials outside the app’s default chain */
  String awsProfile; // e.g., "prod"

  String roleArn; // if you plan to assume-role
}
