package io.ascend.flockr.admin.exception.spark;

import lombok.Getter;

/** Exception thrown when Spark API returns an error response. */
@Getter
public class SparkApiException extends SparkJobException {
  private final int statusCode;
  private final String responseBody;

  public SparkApiException(String message, int statusCode, String responseBody) {
    super(String.format("%s. HTTP Status: %d, Response: %s", message, statusCode, responseBody));
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }
}
