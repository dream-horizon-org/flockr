package com.ascend.flockr.common.exception.errors;

import com.dream11.rest.exception.RestError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of defined error codes used throughout the Flockr platform.
 *
 * <p>This enum implements {@link RestError} to integrate with the Dream11 REST framework's error
 * handling. Each error defines:
 *
 * <ul>
 *   <li><strong>Error Code:</strong> A unique string identifier for the error
 *   <li><strong>Error Message:</strong> A human-readable message (may contain format specifiers
 *       like %s)
 *   <li><strong>HTTP Status Code:</strong> The HTTP status code to return (400, 500, etc.)
 * </ul>
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * throw ExceptionUtil.getException(DefinedErrors.INVALID_EXPIRY_TIME, "2023-01-01 00:00:00");
 * }</pre>
 *
 * <p><strong>Error Categories:</strong>
 *
 * <ul>
 *   <li><strong>4xx (Client Errors):</strong> Invalid requests, bad parameters, validation failures
 *   <li><strong>5xx (Server Errors):</strong> Internal server errors, database failures
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 * @see RestError
 */
@Getter
@RequiredArgsConstructor
public enum DefinedErrors implements RestError {
  /**
   * Internal server error indicating an unexpected failure.
   *
   * <p>HTTP Status: 500
   *
   * <p>Message format: "unknown error occurred due to %s"
   */
  INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "unknown error occurred due to %s", 500),

  /**
   * Request validation error indicating the request violates one or more constraints.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message format: "The request violates one or more constraints: %s"
   */
  INVALID_REQUEST("INVALID_REQUEST", "The request violates one or more constraints: %s", 400),

  /**
   * Request parameter validation error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "Invalid request params"
   */
  INVALID_REQUEST_PARAMS("INVALID_REQUEST_PARAMS", "Invalid request params", 400),

  /**
   * Expiry time validation error indicating the provided expiry time is invalid.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message format: "Invalid expiry time %s"
   *
   * <p>Typically thrown when an expiry timestamp is in the past or has an invalid format.
   */
  INVALID_EXPIRY_TIME("INVALID_EXPIRY_TIME", "Invalid expiry time %s", 400),

  /**
   * Aerospike append operation failure.
   *
   * <p>HTTP Status: 500
   *
   * <p>Message: "append to aerospike failed"
   *
   * <p>Thrown when an append operation to Aerospike fails after retries.
   */
  AEROSPIKE_APPEND_FAILED("AEROSPIKE_APPEND_FAILED", "append to aerospike failed", 500);

  /** Unique error code identifier. */
  private final String errorCode;

  /** Human-readable error message (may contain format specifiers). */
  private final String errorMessage;

  /** HTTP status code to return for this error. */
  private final int httpStatusCode;
}
