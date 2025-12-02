package io.ascend.flockr.admin.exception;

import lombok.Getter;

/**
 * Exception thrown when a user attempts an action they are not authorized to perform.
 *
 * <p>This exception provides a standardized way to handle authorization failures across the module,
 * resulting in HTTP 403 Forbidden responses.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Basic unauthorized access
 * throw new ForbiddenAccessException("User not authorized to update audience owners");
 *
 * // With specific resource context
 * throw new ForbiddenAccessException("UPDATE_OWNER", "User is not an owner of audience 123");
 * }</pre>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Getter
public class ForbiddenAccessException extends RuntimeException {

  /** The error code for API responses. */
  private final String errorCode;

  /**
   * Constructs a ForbiddenAccessException with a default error code.
   *
   * @param message the error message describing why access is forbidden
   */
  public ForbiddenAccessException(String message) {
    super(message);
    this.errorCode = "FORBIDDEN";
  }

  /**
   * Constructs a ForbiddenAccessException with a custom error code.
   *
   * @param errorCode the error code for API responses
   * @param message the error message describing why access is forbidden
   */
  public ForbiddenAccessException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * Constructs a ForbiddenAccessException with a cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public ForbiddenAccessException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "FORBIDDEN";
  }
}
