package com.ascend.flockr.domain.stream;

public enum PropertyType {
  bool("boolean"),

  string("string"),

  number("number");

  private final String literal;

  PropertyType(String literal) {
    this.literal = literal;
  }

  public String literal() {
    return literal;
  }
}
