package io.ascend.flockr.admin.exception;

/** Exception thrown when Flink REST API returns an error. */
public class FlinkApiException extends FlinkClientException {

  public FlinkApiException(String message, int statusCode) {
    super("FLINK_API_ERROR", message, statusCode);
  }

  public FlinkApiException(String message, int statusCode, Throwable cause) {
    super("FLINK_API_ERROR", message, statusCode, cause);
  }

  public FlinkApiException(String endpoint, int statusCode, String responseBody) {
    super(
        "FLINK_API_ERROR",
        String.format(
            "Flink API request failed. Endpoint: %s, Status: %d, Response: %s",
            endpoint, statusCode, responseBody),
        statusCode);
  }
}
