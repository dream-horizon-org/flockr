package io.ascend.flockr.users.exception.errors;

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
 * throw ExceptionUtil.getException(DefinedErrors.INVALID_USER_ID, "abc");
 * }</pre>
 *
 * <p><strong>Error Categories:</strong>
 *
 * <ul>
 *   <li><strong>4xx (Client Errors):</strong> Invalid requests, bad parameters, validation failures
 *   <li><strong>5xx (Server Errors):</strong> Internal server errors, database failures
 * </ul>
 *
 * @author Sudhanshu Rai
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
   * Request parameter validation error (generic).
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "Invalid request params"
   *
   * <p>Note: Prefer using more specific error codes when possible.
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
  AEROSPIKE_APPEND_FAILED("AEROSPIKE_APPEND_FAILED", "append to aerospike failed", 500),

  // ============================================================================
  // Header Validation Errors
  // ============================================================================

  /**
   * Missing userId header error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "Missing required header: userId"
   *
   * <p>Thrown when the userId header is missing from the request.
   */
  MISSING_USER_ID_HEADER("MISSING_USER_ID_HEADER", "Missing required header: userId", 400),

  /**
   * Invalid userId format error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message format: "Invalid userId: %s. userId must be a positive number"
   *
   * <p>Thrown when userId is not a valid positive number.
   */
  MISSING_USER_ID("MISSING_USER_ID", "User Id cannot be empty or null", 400),

  /**
   * Missing x-project-key header error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "Missing required header: x-project-key"
   *
   * <p>Thrown when the x-project-key header is missing from the request.
   */
  MISSING_PROJECT_KEY_HEADER(
      "MISSING_PROJECT_KEY_HEADER", "Missing required header: x-project-key", 400),

  /**
   * Invalid x-project-key format error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message format: "Invalid x-project-key format: %s. Expected format: tenantId_projectId"
   *
   * <p>Thrown when x-project-key does not match the expected format (tenantId_projectId).
   */
  INVALID_PROJECT_KEY_FORMAT(
      "INVALID_PROJECT_KEY_FORMAT",
      "Invalid x-project-key format: %s. Expected format: tenantId_projectId",
      400),

  // ============================================================================
  // Tenant and Project ID Validation Errors
  // ============================================================================

  /**
   * Invalid tenantId format error (not a valid UUID).
   *
   * <p>HTTP Status: 400
   *
   * <p>Message format: "Invalid tenantId: %s. tenantId must be a valid UUID format"
   *
   * <p>Thrown when tenantId does not match UUID format (e.g.,
   * 550e8400-e29b-41d4-a716-446655440000).
   */
  INVALID_TENANT_ID_FORMAT(
      "INVALID_TENANT_ID_FORMAT",
      "Invalid tenantId: %s. tenantId must be a valid UUID format",
      400),

  /**
   * Missing or empty tenantId error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "tenantId is required and cannot be empty"
   *
   * <p>Thrown when tenantId is null, empty, or whitespace-only.
   */
  MISSING_TENANT_ID("MISSING_TENANT_ID", "tenantId is required and cannot be empty", 400),

  /**
   * Missing or empty projectId error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "projectId is required and cannot be empty"
   *
   * <p>Thrown when projectId is null, empty, or whitespace-only.
   */
  MISSING_PROJECT_ID("MISSING_PROJECT_ID", "projectId is required and cannot be empty", 400),

  // ============================================================================
  // Request Body Validation Errors
  // ============================================================================

  /**
   * Missing cohort_key in request body error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "Missing required field: cohort_key"
   *
   * <p>Thrown when cohort_key field is missing or empty in the request body.
   */
  MISSING_COHORT_KEY("MISSING_COHORT_KEY", "Missing required field: cohort_key", 400),

  /**
   * Missing action in request body error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "Missing required field: action"
   *
   * <p>Thrown when action field is missing or empty in the request body.
   */
  MISSING_ACTION("MISSING_ACTION", "Missing required field: action", 400),

  /**
   * Missing expire_at in request body error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "Missing required field: expire_at"
   *
   * <p>Thrown when expire_at field is missing or empty in the request body.
   */
  MISSING_EXPIRE_AT("MISSING_EXPIRE_AT", "Missing required field: expire_at", 400),

  /**
   * Invalid action value error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message format: "Invalid action: %s. action must be either 'append' or 'remove'"
   *
   * <p>Thrown when action value is not "append" or "remove".
   */
  INVALID_ACTION(
      "INVALID_ACTION", "Invalid action: %s. action must be either 'append' or 'remove'", 400),

  // ============================================================================
  // Bulk Assignment Form Data Validation Errors
  // ============================================================================

  /**
   * Missing csv_file in multipart form error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "Missing required form field: csv_file"
   *
   * <p>Thrown when csv_file is missing from multipart form data.
   */
  MISSING_CSV_FILE("MISSING_CSV_FILE", "Missing required form field: csv_file", 400),

  /**
   * Missing cohort_name in multipart form error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "Missing required form field: cohort_name"
   *
   * <p>Thrown when cohort_name is missing from multipart form data.
   */
  MISSING_COHORT_NAME("MISSING_COHORT_NAME", "Missing required form field: cohort_name", 400),

  /**
   * Empty CSV file error.
   *
   * <p>HTTP Status: 400
   *
   * <p>Message: "Uploaded CSV file is empty"
   *
   * <p>Thrown when the uploaded CSV file has no content.
   */
  EMPTY_CSV_FILE("EMPTY_CSV_FILE", "Uploaded CSV file is empty", 400);

  /** Unique error code identifier. */
  private final String errorCode;

  /** Human-readable error message (may contain format specifiers). */
  private final String errorMessage;

  /** HTTP status code to return for this error. */
  private final int httpStatusCode;
}
