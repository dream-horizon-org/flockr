package com.ascend.flockr.model.task.constant;

import java.util.Arrays;
import java.util.Optional;

public enum TaskType {
  eventStream("real-time"),

  storedData("historic");

  private final String ref;

  TaskType(String ref) {
    this.ref = ref;
  }

  public static TaskType fromRef(String ref) {
    Optional<TaskType> taskType =
        Arrays.stream(TaskType.values()).filter(i -> i.ref().equals(ref)).findAny();
    return taskType.orElse(null);
  }

  public String ref() {
    return ref;
  }
}
