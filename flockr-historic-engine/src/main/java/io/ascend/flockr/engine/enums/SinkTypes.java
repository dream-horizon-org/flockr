package io.ascend.flockr.engine.enums;

public enum SinkTypes {
  KAFKA("kafka"),
  S3("s3"),
  API("api"),
  WEBHOOK("webhook");

  SinkTypes(String configType) {}
}
