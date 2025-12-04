package io.ascend.flockr.admin.exception;

import lombok.Getter;

/**
 * Unified exception class for all Flink client operations.
 *
 * <p>This exception consolidates all Flink-related errors with specific error codes:
 *
 * <ul>
 *   <li>{@code FLINK_CONNECTION_FAILED} - Connection to Flink cluster failed (503)
 *   <li>{@code FLINK_JOB_NOT_FOUND} - Job not found (404)
 *   <li>{@code FLINK_JOB_SUBMISSION_FAILED} - Job submission failed (500)
 *   <li>{@code FLINK_JAR_UPLOAD_FAILED} - JAR upload failed (500)
 *   <li>{@code FLINK_SAVEPOINT_FAILED} - Savepoint operation failed (500)
 *   <li>{@code FLINK_API_ERROR} - General API error (varies)
 *   <li>{@code FLINK_ERROR} - Generic Flink error (500)
 * </ul>
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Connection failure
 * throw FlinkClientException.connectionFailed("localhost", 8081);
 *
 * // Job not found
 * throw FlinkClientException.jobNotFound("job-123");
 *
 * // Job submission failure
 * throw FlinkClientException.jobSubmissionFailed("jar-id", "EntryClass", cause);
 *
 * // JAR upload failure
 * throw FlinkClientException.jarUploadFailed("/path/to/jar.jar", 500);
 *
 * // Savepoint failure
 * throw FlinkClientException.savepointFailed("job-123", "trigger", "Timeout");
 *
 * // API error
 * throw FlinkClientException.apiError("/jobs/123", 500, "Internal error");
 * }</pre>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Getter
public class FlinkClientException extends RuntimeException {

  /** Error code constants */
  public static final String CONNECTION_FAILED = "FLINK_CONNECTION_FAILED";

  public static final String JOB_NOT_FOUND = "FLINK_JOB_NOT_FOUND";
  public static final String JOB_SUBMISSION_FAILED = "FLINK_JOB_SUBMISSION_FAILED";
  public static final String JAR_UPLOAD_FAILED = "FLINK_JAR_UPLOAD_FAILED";
  public static final String SAVEPOINT_FAILED = "FLINK_SAVEPOINT_FAILED";
  public static final String API_ERROR = "FLINK_API_ERROR";
  public static final String GENERIC_ERROR = "FLINK_ERROR";

  private final String errorCode;
  private final int statusCode;

  /**
   * Creates a FlinkClientException with default error code and 500 status.
   *
   * @param message the error message
   */
  public FlinkClientException(String message) {
    super(message);
    this.errorCode = GENERIC_ERROR;
    this.statusCode = 500;
  }

  /**
   * Creates a FlinkClientException with default error code, 500 status, and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public FlinkClientException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = GENERIC_ERROR;
    this.statusCode = 500;
  }

  /**
   * Creates a FlinkClientException with custom error code and status.
   *
   * @param errorCode the error code
   * @param message the error message
   * @param statusCode the HTTP status code
   */
  public FlinkClientException(String errorCode, String message, int statusCode) {
    super(message);
    this.errorCode = errorCode;
    this.statusCode = statusCode;
  }

  /**
   * Creates a FlinkClientException with custom error code, status, and cause.
   *
   * @param errorCode the error code
   * @param message the error message
   * @param statusCode the HTTP status code
   * @param cause the underlying cause
   */
  public FlinkClientException(String errorCode, String message, int statusCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.statusCode = statusCode;
  }

  // ========== Factory Methods ==========

  /**
   * Creates an exception for Flink connection failures.
   *
   * @param host the Flink host
   * @param port the Flink port
   * @return FlinkClientException with 503 status
   */
  public static FlinkClientException connectionFailed(String host, int port) {
    return new FlinkClientException(
        CONNECTION_FAILED,
        String.format("Failed to connect to Flink cluster at %s:%d", host, port),
        503);
  }

  /**
   * Creates an exception for Flink connection failures with cause.
   *
   * @param host the Flink host
   * @param port the Flink port
   * @param cause the underlying cause
   * @return FlinkClientException with 503 status
   */
  public static FlinkClientException connectionFailed(String host, int port, Throwable cause) {
    return new FlinkClientException(
        CONNECTION_FAILED,
        String.format("Failed to connect to Flink cluster at %s:%d", host, port),
        503,
        cause);
  }

  /**
   * Creates an exception for Flink connection failures with message.
   *
   * @param message the error message
   * @return FlinkClientException with 503 status
   */
  public static FlinkClientException connectionFailed(String message) {
    return new FlinkClientException(CONNECTION_FAILED, message, 503);
  }

  /**
   * Creates an exception for job not found.
   *
   * @param jobId the job ID that was not found
   * @return FlinkClientException with 404 status
   */
  public static FlinkClientException jobNotFound(String jobId) {
    return new FlinkClientException(
        JOB_NOT_FOUND, String.format("Flink job not found: %s", jobId), 404);
  }

  /**
   * Creates an exception for job submission failures.
   *
   * @param jarId the JAR ID
   * @param entryClass the entry class
   * @param cause the underlying cause
   * @return FlinkClientException with 500 status
   */
  public static FlinkClientException jobSubmissionFailed(
      String jarId, String entryClass, Throwable cause) {
    return new FlinkClientException(
        JOB_SUBMISSION_FAILED,
        String.format(
            "Failed to submit Flink job with jarId: %s, entryClass: %s", jarId, entryClass),
        500,
        cause);
  }

  /**
   * Creates an exception for job submission failures.
   *
   * @param message the error message
   * @return FlinkClientException with 500 status
   */
  public static FlinkClientException jobSubmissionFailed(String message) {
    return new FlinkClientException(JOB_SUBMISSION_FAILED, message, 500);
  }

  /**
   * Creates an exception for JAR upload failures.
   *
   * @param jarFilePath the JAR file path
   * @param statusCode the HTTP status code from the upload attempt
   * @return FlinkClientException with the given status code
   */
  public static FlinkClientException jarUploadFailed(String jarFilePath, int statusCode) {
    return new FlinkClientException(
        JAR_UPLOAD_FAILED,
        String.format("Failed to upload JAR file: %s. Status code: %d", jarFilePath, statusCode),
        statusCode);
  }

  /**
   * Creates an exception for JAR upload failures with cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @return FlinkClientException with 500 status
   */
  public static FlinkClientException jarUploadFailed(String message, Throwable cause) {
    return new FlinkClientException(JAR_UPLOAD_FAILED, message, 500, cause);
  }

  /**
   * Creates an exception for savepoint operation failures.
   *
   * @param jobId the job ID
   * @param operation the operation that failed (e.g., "trigger", "cancel with savepoint")
   * @param failureCause the reason for failure
   * @return FlinkClientException with 500 status
   */
  public static FlinkClientException savepointFailed(
      String jobId, String operation, String failureCause) {
    return new FlinkClientException(
        SAVEPOINT_FAILED,
        String.format("Savepoint %s failed for job %s. Reason: %s", operation, jobId, failureCause),
        500);
  }

  /**
   * Creates an exception for savepoint operation failures.
   *
   * @param message the error message
   * @return FlinkClientException with 500 status
   */
  public static FlinkClientException savepointFailed(String message) {
    return new FlinkClientException(SAVEPOINT_FAILED, message, 500);
  }

  /**
   * Creates an exception for API errors.
   *
   * @param endpoint the API endpoint
   * @param statusCode the HTTP status code
   * @param responseBody the response body
   * @return FlinkClientException with the given status code
   */
  public static FlinkClientException apiError(
      String endpoint, int statusCode, String responseBody) {
    return new FlinkClientException(
        API_ERROR,
        String.format(
            "Flink API request failed. Endpoint: %s, Status: %d, Response: %s",
            endpoint, statusCode, responseBody),
        statusCode);
  }

  /**
   * Creates an exception for API errors with cause.
   *
   * @param message the error message
   * @param statusCode the HTTP status code
   * @param cause the underlying cause
   * @return FlinkClientException with the given status code
   */
  public static FlinkClientException apiError(String message, int statusCode, Throwable cause) {
    return new FlinkClientException(API_ERROR, message, statusCode, cause);
  }
}
