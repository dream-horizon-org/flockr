package io.ascend.flockr.users.dto;

import lombok.Getter;

/**
 * Standard response entity wrapper for API responses.
 *
 * <p>Provides a consistent response structure with either success data or error information.
 *
 * @param <T> the type of data in success response
 * @author Sudhanshu Rai
 * @since 1.0
 */
public class ResponseEntity {

  /**
   * Success response containing data.
   *
   * @param <T> the type of data
   * @param data the response data
   */
  public record Success<T>(T data) {}

  /** Failure response containing error information. */
  @Getter
  public static class Failure {
    private final ErrorEntity error;

    /**
     * Creates a failure response.
     *
     * @param code error code
     * @param message error message
     * @param cause error cause (optional)
     */
    public Failure(String code, String message, String cause) {
      this.error = new ErrorEntity(code, message, cause);
    }

    /**
     * Error entity containing error details.
     *
     * @param code error code
     * @param message error message
     * @param cause error cause
     */
    public record ErrorEntity(String code, String message, String cause) {}
  }
}
