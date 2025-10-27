package com.ascend.flockr.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class Error {
  private final String code;
  private final String message;

  public static Error of(String code, String message) {
    return new Error(code, message);
  }
}
