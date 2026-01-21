package io.ascend.flockr.engine.enums;

import lombok.Getter;

@Getter
public enum SourceTypes {
  ATHENA("athena");

  private final String configType;

  SourceTypes(String configType) {
    this.configType = configType;
  }
}
