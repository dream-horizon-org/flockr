package com.ascend.flockr.exception;

import com.dream11.rest.exception.RestError;
import com.dream11.rest.exception.RestException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.HttpStatus;

/**
 * Enumeration of application error codes with their associated HTTP status codes and messages.
 *
 * <p>This enum implements {@link RestError} to provide standardized error handling across the
 * application. Each error code includes:
 *
 * <ul>
 *   <li>A unique error code string
 *   <li>A human-readable error message
 *   <li>An HTTP status code
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum ErrorEnum implements RestError {
  REST_HEALTH_CHECK_FAILED(
      "flockr_REST_HEALTH_CHECK_FAILED",
      "HealthCheck Failed for flockr service",
      HttpStatus.SC_INTERNAL_SERVER_ERROR),

  AUDIENCE_NOT_FOUND("AUDIENCE_NOT_FOUND", "Audience not found", HttpStatus.SC_NOT_FOUND),

  RULE_NOT_FOUND("RULE_NOT_FOUND", "Rule not found", HttpStatus.SC_NOT_FOUND),

  INVALID_REQUEST_BODY(
      "INVALID_REQUEST_BODY", "Invalid request body parameters", HttpStatus.SC_BAD_REQUEST),

  CONFIG_VALIDATION_FAILED(
      "CONFIG_VALIDATION_FAILED",
      "Connector configuration validation failed",
      HttpStatus.SC_BAD_REQUEST),

  CONFIG_PARSING_FAILED(
      "CONFIG_PARSING_FAILED",
      "Failed to parse connector configuration",
      HttpStatus.SC_BAD_REQUEST),

  CONFIG_VALIDATOR_NOT_FOUND(
      "CONFIG_VALIDATOR_NOT_FOUND",
      "No validator found for the specified connector type",
      HttpStatus.SC_BAD_REQUEST),

  DATABASE_ERROR(
      "DATABASE_ERROR", "Database operation failed", HttpStatus.SC_INTERNAL_SERVER_ERROR),

  INTERNAL_ERROR(
      "INTERNAL_ERROR",
      "An unexpected internal error occurred",
      HttpStatus.SC_INTERNAL_SERVER_ERROR);

  /** The unique error code identifier. */
  private final String errorCode;

  /** Human-readable error message. */
  private final String errorMessage;

  /** HTTP status code associated with this error. */
  private final int httpStatusCode;

  /**
   * Handles an exception by returning it if it's already a RestException, otherwise returns the
   * default exception.
   *
   * @param throwable the exception to handle
   * @param defaultException the default exception to return if throwable is not a RestException
   * @return the throwable if it's a RestException, otherwise the defaultException
   */
  public static RestException handleException(Throwable throwable, RestException defaultException) {
    if (throwable instanceof RestException restException) return restException;
    else return defaultException;
  }
}
