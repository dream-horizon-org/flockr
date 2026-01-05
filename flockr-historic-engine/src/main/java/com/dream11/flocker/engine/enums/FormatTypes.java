package com.dream11.flocker.engine.enums;

public enum FormatTypes {
  CSV("csv"),
  PARQUET("parquet"),
  JSON("json"),
  AVRO("avro");

  private final String format;

  FormatTypes(String format) {
    this.format = format;
  }

  public String getFormat() {
    return this.format;
  }
}
