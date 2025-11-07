package com.ascend.flockr.domain.task.enums;

import java.util.Arrays;
import java.util.Optional;

public enum TaskType {
  eventStream("real-time"),

  storedData("historic");

  private final String ref;

  TaskType(String ref) {
    this.ref = ref;
  }

  /** Lookup by reference string. */
  public static Optional<TaskType> fromRef(String ref) {
    if (ref == null) {
      return Optional.empty();
    }
    return Arrays.stream(TaskType.values()).filter(i -> i.ref.equals(ref)).findAny();
  }

  /** Backwards-compatible accessor kept for existing callers. */
  public String ref() {
    return ref;
  }

  /** Standard JavaBean getter. */
  public String getRef() {
    return ref;
  }
}
