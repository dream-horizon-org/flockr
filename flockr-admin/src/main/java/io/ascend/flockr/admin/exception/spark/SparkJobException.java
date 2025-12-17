package io.ascend.flockr.admin.exception.spark;

/** Base exception class for all Spark-related errors. */
public class SparkJobException extends RuntimeException {
  public SparkJobException(String message) {
    super(message);
  }

  public SparkJobException(String message, Throwable cause) {
    super(message, cause);
  }
}
