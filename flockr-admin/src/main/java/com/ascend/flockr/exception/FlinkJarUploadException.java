package com.ascend.flockr.exception;

/** Exception thrown when JAR upload to Flink cluster fails. */
public class FlinkJarUploadException extends FlinkClientException {

  public FlinkJarUploadException(String message) {
    super("FLINK_JAR_UPLOAD_FAILED", message, 500);
  }

  public FlinkJarUploadException(String message, Throwable cause) {
    super("FLINK_JAR_UPLOAD_FAILED", message, 500, cause);
  }

  public FlinkJarUploadException(String jarFilePath, int statusCode) {
    super(
        "FLINK_JAR_UPLOAD_FAILED",
        String.format("Failed to upload JAR file: %s. Status code: %d", jarFilePath, statusCode),
        statusCode);
  }
}
