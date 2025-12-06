package io.ascend.flockr.admin.io;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;

/**
 * Container classes for API response entities.
 *
 * <p>This class provides standardized response wrappers for both successful and failed API
 * operations, ensuring consistent response structure across all endpoints.
 *
 * @since 1.0
 */
public final class ResponseEntity {

  /**
   * Response wrapper for successful API operations.
   *
   * @param data the payload data returned by the operation
   * @param <T> the type of the response data
   */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record Success<T>(T data) {}

  /**
   * Response wrapper for failed API operations.
   *
   * <p>Contains an error entity with code, message, and cause information.
   */
  @Getter
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Failure {
    /** The error details. */
    private final ErrorEntity error;

    /**
     * Constructs a new failure response.
     *
     * @param code the error code
     * @param message the human-readable error message
     * @param cause additional cause information (may be null)
     */
    public Failure(String code, String message, String cause) {
      this.error = new ErrorEntity(code, message, cause);
    }

    /**
     * Record containing error details.
     *
     * @param code the unique error code
     * @param message a human-readable error message
     * @param cause additional cause information (may be null)
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ErrorEntity(String code, String message, String cause) {}
  }
}
