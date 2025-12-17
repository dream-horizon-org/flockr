package io.ascend.flockr.admin.exception.spark;

/** Exception thrown when connection to Spark cluster fails. */
public class SparkConnectionException extends SparkJobException {
  public SparkConnectionException(String message) {
    super(message);
  }
}
