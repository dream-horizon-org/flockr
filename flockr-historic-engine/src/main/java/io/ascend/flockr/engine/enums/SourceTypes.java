package io.ascend.flockr.engine.enums;

public enum SourceTypes {
  S3("s3"),
  REDSHIFT("redshift"),
  ATHENA("athena"),
  KAFKA("kafka");

  private final String configType;

  SourceTypes(String configType) {
    this.configType = configType;
  }

  public String getConfigType() {
    return configType;
  }
}
