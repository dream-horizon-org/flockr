package io.ascend.flockr.admin.client.spark;

import lombok.Getter;

/**
 * Unified exception for all Spark-related errors.
 *
 * <p>This exception consolidates all Spark error scenarios into a single class with categorized
 * error types. It provides factory methods for common error scenarios and optional context fields
 * for debugging.
 *
 * <p><b>Design Rationale:</b>
 *
 * <ul>
 *   <li>Single exception type simplifies error handling in calling code
 *   <li>Error type enum enables categorization without class proliferation
 *   <li>Optional fields (statusCode, responseBody) provide rich context when available
 *   <li>Factory methods provide clear, intention-revealing ways to create exceptions
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>{@code
 * // API error with HTTP details
 * throw SparkException.apiError("Request failed", 500, responseBody);
 *
 * // Connection failure
 * throw SparkException.connectionError("Cluster unreachable", cause);
 *
 * // Job not found
 * throw SparkException.jobNotFound("app-123456");
 *
 * // Job submission failure
 * throw SparkException.submissionError("Invalid configuration", cause);
 * }</pre>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Getter
public class SparkException extends RuntimeException {

  /** The category of error that occurred */
  private final ErrorType errorType;

  /** HTTP status code (populated for API errors) */
  private final Integer statusCode;

  /** HTTP response body (populated for API errors) */
  private final String responseBody;

  /** Job or application identifier related to the error */
  private final String jobId;

  /**
   * Categorizes different types of Spark errors.
   *
   * <p>This enum allows error handling code to respond differently to different error categories
   * without needing separate exception classes.
   */
  public enum ErrorType {
    /** HTTP API returned an error response */
    API_ERROR,

    /** Failed to connect to Spark cluster */
    CONNECTION_ERROR,

    /** Job submission failed */
    SUBMISSION_ERROR,

    /** Requested job was not found */
    JOB_NOT_FOUND,

    /** Generic Spark operation failure */
    OPERATION_ERROR
  }

  /**
   * Private constructor - use factory methods instead.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @param errorType the category of error
   * @param statusCode HTTP status code (optional)
   * @param responseBody HTTP response body (optional)
   * @param jobId job/application ID (optional)
   */
  private SparkException(
      String message,
      Throwable cause,
      ErrorType errorType,
      Integer statusCode,
      String responseBody,
      String jobId) {
    super(message, cause);
    this.errorType = errorType;
    this.statusCode = statusCode;
    this.responseBody = responseBody;
    this.jobId = jobId;
  }

  // ==================== Factory Methods ====================

  /**
   * Creates an exception for Spark API errors (HTTP errors).
   *
   * @param message the error message
   * @param statusCode the HTTP status code
   * @param responseBody the HTTP response body
   * @return a SparkException with API error details
   */
  public static SparkException apiError(String message, int statusCode, String responseBody) {
    String fullMessage =
        String.format("%s. HTTP Status: %d, Response: %s", message, statusCode, responseBody);
    return new SparkException(
        fullMessage, null, ErrorType.API_ERROR, statusCode, responseBody, null);
  }

  /**
   * Creates an exception for connection failures to Spark cluster.
   *
   * @param message the error message
   * @return a SparkException for connection errors
   */
  public static SparkException connectionError(String message) {
    return new SparkException(message, null, ErrorType.CONNECTION_ERROR, null, null, null);
  }

  /**
   * Creates an exception for connection failures with an underlying cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @return a SparkException for connection errors
   */
  public static SparkException connectionError(String message, Throwable cause) {
    return new SparkException(message, cause, ErrorType.CONNECTION_ERROR, null, null, null);
  }

  /**
   * Creates an exception for job submission failures.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @return a SparkException for submission errors
   */
  public static SparkException submissionError(String message, Throwable cause) {
    String fullMessage = "Failed to submit Spark job: " + message;
    return new SparkException(fullMessage, cause, ErrorType.SUBMISSION_ERROR, null, null, null);
  }

  /**
   * Creates an exception when a Spark job is not found.
   *
   * @param jobId the job or application ID that was not found
   * @return a SparkException for job not found errors
   */
  public static SparkException jobNotFound(String jobId) {
    String message = String.format("Spark job not found: %s", jobId);
    return new SparkException(message, null, ErrorType.JOB_NOT_FOUND, 404, null, jobId);
  }

  /**
   * Creates a generic Spark operation error.
   *
   * @param message the error message
   * @return a SparkException for generic operation errors
   */
  public static SparkException operationError(String message) {
    return new SparkException(message, null, ErrorType.OPERATION_ERROR, null, null, null);
  }

  /**
   * Creates a generic Spark operation error with an underlying cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @return a SparkException for generic operation errors
   */
  public static SparkException operationError(String message, Throwable cause) {
    return new SparkException(message, cause, ErrorType.OPERATION_ERROR, null, null, null);
  }

  // ==================== Convenience Methods ====================

  /**
   * Checks if this exception is an API error.
   *
   * @return true if this is an API error
   */
  public boolean isApiError() {
    return errorType == ErrorType.API_ERROR;
  }

  /**
   * Checks if this exception is a connection error.
   *
   * @return true if this is a connection error
   */
  public boolean isConnectionError() {
    return errorType == ErrorType.CONNECTION_ERROR;
  }

  /**
   * Checks if this exception is a submission error.
   *
   * @return true if this is a submission error
   */
  public boolean isSubmissionError() {
    return errorType == ErrorType.SUBMISSION_ERROR;
  }

  /**
   * Checks if this exception is a job not found error.
   *
   * @return true if this is a job not found error
   */
  public boolean isJobNotFound() {
    return errorType == ErrorType.JOB_NOT_FOUND;
  }

  /**
   * Checks if this error is retryable (connection or server errors).
   *
   * @return true if the operation should be retried
   */
  public boolean isRetryable() {
    return errorType == ErrorType.CONNECTION_ERROR
        || (errorType == ErrorType.API_ERROR && statusCode != null && statusCode >= 500);
  }
}
