package com.ascend.flockr.util;

public class Utility {
  public static <T> T getOrDefault(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }
}
