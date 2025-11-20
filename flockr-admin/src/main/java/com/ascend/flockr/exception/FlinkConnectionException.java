package com.ascend.flockr.exception;

/** Exception thrown when connection to Flink cluster fails. */
public class FlinkConnectionException extends FlinkClientException {

  public FlinkConnectionException(String message) {
    super("FLINK_CONNECTION_FAILED", message, 503);
  }

  public FlinkConnectionException(String message, Throwable cause) {
    super("FLINK_CONNECTION_FAILED", message, 503, cause);
  }

  public FlinkConnectionException(String host, int port, Throwable cause) {
    super(
        "FLINK_CONNECTION_FAILED",
        String.format("Failed to connect to Flink cluster at %s:%d", host, port),
        503,
        cause);
  }
}
