package io.ascend.flockr.admin.exception;

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
 * <p>Usage example:
 *
 * <pre>{@code
 * // Creating a RestException from an error enum
 * throw ErrorEnum.AUDIENCE_NOT_FOUND.toException();
 *
 * // With custom message
 * throw ErrorEnum.AUDIENCE_NOT_FOUND.toException("Audience with ID 123 not found");
 * }</pre>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum ErrorEnum implements RestError {

  // Health Check Errors
  REST_HEALTH_CHECK_FAILED(
      "HEALTH_CHECK_FAILED",
      "HealthCheck failed for flockr service",
      HttpStatus.SC_INTERNAL_SERVER_ERROR),

  // Resource Not Found Errors (404)
  AUDIENCE_NOT_FOUND("AUDIENCE_NOT_FOUND", "Audience not found", HttpStatus.SC_NOT_FOUND),

  RULE_NOT_FOUND("RULE_NOT_FOUND", "Rule not found", HttpStatus.SC_NOT_FOUND),

  DATA_SOURCE_NOT_FOUND("DATA_SOURCE_NOT_FOUND", "Data source not found", HttpStatus.SC_NOT_FOUND),

  DATA_SINK_NOT_FOUND("DATA_SINK_NOT_FOUND", "Data sink not found", HttpStatus.SC_NOT_FOUND),

  SINK_NOT_ACTIVE(
      "SINK_NOT_ACTIVE", "One or more specified sinks are not active", HttpStatus.SC_BAD_REQUEST),

  OWNER_NOT_FOUND("OWNER_NOT_FOUND", "Owner not found", HttpStatus.SC_NOT_FOUND),

  RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Requested resource not found", HttpStatus.SC_NOT_FOUND),

  // Conflict Errors (409)
  AUDIENCE_ALREADY_EXISTS(
      "AUDIENCE_ALREADY_EXISTS",
      "An audience with the same name already exists in this project",
      HttpStatus.SC_CONFLICT),

  DUPLICATE_OWNER(
      "DUPLICATE_OWNER", "User is already an owner of this audience", HttpStatus.SC_CONFLICT),

  DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", "Resource already exists", HttpStatus.SC_CONFLICT),

  // Forbidden Errors (403)
  FORBIDDEN("FORBIDDEN", "Access denied", HttpStatus.SC_FORBIDDEN),

  NOT_AUTHORIZED(
      "NOT_AUTHORIZED", "User not authorized to perform this action", HttpStatus.SC_FORBIDDEN),

  // Bad Request Errors (400)
  INVALID_REQUEST_BODY(
      "INVALID_REQUEST_BODY", "Invalid request body parameters", HttpStatus.SC_BAD_REQUEST),

  INVALID_ARGUMENT("INVALID_ARGUMENT", "Invalid argument provided", HttpStatus.SC_BAD_REQUEST),

  MISSING_REQUIRED_FIELD(
      "MISSING_REQUIRED_FIELD", "Required field is missing", HttpStatus.SC_BAD_REQUEST),

  INVALID_REFERENCE(
      "INVALID_REFERENCE", "Referenced resource does not exist", HttpStatus.SC_BAD_REQUEST),

  CONSTRAINT_VIOLATION("CONSTRAINT_VIOLATION", "Data validation failed", HttpStatus.SC_BAD_REQUEST),

  AUDIENCE_EXPIRED(
      "AUDIENCE_EXPIRED", "Audience is expired and cannot be modified", HttpStatus.SC_BAD_REQUEST),

  LAST_OWNER(
      "LAST_OWNER", "Cannot remove the last owner of an audience", HttpStatus.SC_BAD_REQUEST),

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

  // Audience Type Errors (400)
  RULES_NOT_ALLOWED_FOR_STATIC_AUDIENCE(
      "RULES_NOT_ALLOWED",
      "Rules cannot be added to STATIC audiences. Use CSV import instead.",
      HttpStatus.SC_BAD_REQUEST),

  IMPORT_NOT_ALLOWED_FOR_CONDITIONAL_AUDIENCE(
      "IMPORT_NOT_ALLOWED",
      "CSV import is not allowed for CONDITIONAL audiences. Use rules instead.",
      HttpStatus.SC_BAD_REQUEST),

  INVALID_AUDIENCE_TYPE(
      "INVALID_AUDIENCE_TYPE",
      "Invalid audience type. Must be CONDITIONAL or STATIC.",
      HttpStatus.SC_BAD_REQUEST),

  IMPORT_NOT_FOUND("IMPORT_NOT_FOUND", "Import record not found", HttpStatus.SC_NOT_FOUND),

  INVALID_CSV_FORMAT(
      "INVALID_CSV_FORMAT",
      "The uploaded file is not a valid CSV format",
      HttpStatus.SC_BAD_REQUEST),

  IMPORT_ALREADY_PROCESSING(
      "IMPORT_ALREADY_PROCESSING",
      "An import is already in progress for this audience",
      HttpStatus.SC_CONFLICT),

  // Server Errors (500)
  DATABASE_ERROR(
      "DATABASE_ERROR", "Database operation failed", HttpStatus.SC_INTERNAL_SERVER_ERROR),

  INTERNAL_ERROR(
      "INTERNAL_ERROR",
      "An unexpected internal error occurred",
      HttpStatus.SC_INTERNAL_SERVER_ERROR),

  // Service Unavailable (503)
  SERVICE_UNAVAILABLE(
      "SERVICE_UNAVAILABLE", "Service is currently unavailable", HttpStatus.SC_SERVICE_UNAVAILABLE);

  /** The unique error code identifier. */
  private final String errorCode;

  /** Human-readable error message. */
  private final String errorMessage;

  /** HTTP status code associated with this error. */
  private final int httpStatusCode;

  /**
   * Creates a RestException from this error enum with the default message.
   *
   * @return a new RestException based on this error
   */
  public RestException toException() {
    return new RestException(errorCode, errorMessage, httpStatusCode);
  }

  /**
   * Creates a RestException from this error enum with a custom message.
   *
   * @param customMessage the custom message to use instead of the default
   * @return a new RestException with the custom message
   */
  public RestException toException(String customMessage) {
    return new RestException(errorCode, customMessage, httpStatusCode);
  }

  /**
   * Creates a RestException from this error enum with a cause.
   *
   * @param cause the underlying cause of the exception
   * @return a new RestException with the cause
   */
  public RestException toException(Throwable cause) {
    return new RestException(errorCode, errorMessage, httpStatusCode, cause);
  }

  /**
   * Creates a RestException from this error enum with a custom message and cause.
   *
   * @param customMessage the custom message to use
   * @param cause the underlying cause of the exception
   * @return a new RestException with the custom message and cause
   */
  public RestException toException(String customMessage, Throwable cause) {
    return new RestException(errorCode, customMessage, httpStatusCode, cause);
  }

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
