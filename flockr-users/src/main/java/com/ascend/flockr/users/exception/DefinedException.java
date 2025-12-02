package com.ascend.flockr.users.exception;

/**
 * Base exception class for defined exceptions in the Flockr platform.
 *
 * <p>This class extends {@link RuntimeException} and serves as a base for application-specific
 * exceptions. It provides a simple constructor that accepts an error message.
 *
 * <p><strong>Usage:</strong>
 *
 * <p>This exception is typically thrown when application-level errors occur that should be
 * propagated to the caller. For more specific error handling, consider using {@link
 * com.ascend.flockr.users.exception.errors.DefinedErrors} with the REST framework's exception
 * handling.
 *
 * @author Flockr Team
 * @since 1.0
 * @see com.ascend.flockr.users.exception.errors.DefinedErrors
 */
public class DefinedException extends RuntimeException {
  /**
   * Constructs a new DefinedException with the specified error message.
   *
   * @param message the error message describing the exception
   */
  public DefinedException(String message) {
    super(message);
  }
}
