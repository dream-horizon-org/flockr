package io.ascend.flockr.engine.enums;

public enum SinkTypes {
  KAFKA("kafka"),
  S3("s3"),
  API("api"),
  WEBHOOK("webhook");

  private final String configType;

  SinkTypes(String configType) {
    this.configType = configType;
  }

  public String getConfigType() {
    return configType;
  }
}
