package com.ascend.flockr.exception;

/** Exception thrown when a Flink job is not found. */
public class FlinkJobNotFoundException extends FlinkClientException {

  public FlinkJobNotFoundException(String jobId) {
    super("FLINK_JOB_NOT_FOUND", String.format("Flink job not found: %s", jobId), 404);
  }

  public FlinkJobNotFoundException(String jobId, Throwable cause) {
    super("FLINK_JOB_NOT_FOUND", String.format("Flink job not found: %s", jobId), 404, cause);
  }
}

