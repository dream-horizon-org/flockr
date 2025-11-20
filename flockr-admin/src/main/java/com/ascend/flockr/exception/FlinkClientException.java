package com.ascend.flockr.exception;

/**
 * Base exception class for Flink client operations. All Flink-specific exceptions should extend
 * this class.
 */
public class FlinkClientException extends RuntimeException {

  private final String errorCode;
  private final int statusCode;

  public FlinkClientException(String message) {
    super(message);
    this.errorCode = "FLINK_ERROR";
    this.statusCode = 500;
  }

  public FlinkClientException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "FLINK_ERROR";
    this.statusCode = 500;
  }

  public FlinkClientException(String errorCode, String message, int statusCode) {
    super(message);
    this.errorCode = errorCode;
    this.statusCode = statusCode;
  }

  public FlinkClientException(String errorCode, String message, int statusCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.statusCode = statusCode;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
