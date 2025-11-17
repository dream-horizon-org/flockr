package com.ascend.flockr.exception;

/** Exception thrown when Flink savepoint operations fail. */
public class FlinkSavepointException extends FlinkClientException {

  public FlinkSavepointException(String message) {
    super("FLINK_SAVEPOINT_FAILED", message, 500);
  }

  public FlinkSavepointException(String message, Throwable cause) {
    super("FLINK_SAVEPOINT_FAILED", message, 500, cause);
  }

  public FlinkSavepointException(String jobId, String operation, String failureCause) {
    super(
        "FLINK_SAVEPOINT_FAILED",
        String.format("Savepoint %s failed for job %s. Reason: %s", operation, jobId, failureCause),
        500);
  }
}
