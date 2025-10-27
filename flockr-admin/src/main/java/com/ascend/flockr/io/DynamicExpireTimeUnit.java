package com.ascend.flockr.io;

public enum DynamicExpireTimeUnit {
  hour("h"),

  day("d");

  private final String value;

  DynamicExpireTimeUnit(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }
}
