package com.ascend.flockr.service.flink;

public enum FlinkJobState {
  SUBMITTED,

  INITIALIZING,

  CREATED,

  RUNNING,

  FAILING,

  FAILED,

  CANCELLING,

  CANCELED,

  FINISHED,

  RESTARTING,

  SUSPENDED,

  RECONCILING
}
