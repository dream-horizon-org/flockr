package io.ascend.flockr.admin.util;

import com.ascend.flockr.exception.ConfigParsingException;
import com.ascend.flockr.exception.ConfigValidationException;
import com.ascend.flockr.io.ResponseEntity;
import com.dream11.rest.exception.RestException;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

/**
 * Utility class for handling errors in REST API flows. Provides generic error handling that can be
 * reused across all controllers.
 */
@Slf4j
@UtilityClass
public class ErrorHandler {

  /**
   * Wraps a reactive Single operation with error handling and maps to ResponseEntity.Success. This
   * is the primary method to use in REST controllers for consistent error handling.
   *
   * @param single The reactive Single operation from service layer
   * @param operationName Name of the operation for logging purposes
   * @param <T> The type of data being returned
   * @return CompletionStage with ResponseEntity.Success or throws RestException on error
   */
  public static <T> CompletionStage<ResponseEntity.Success<T>> handleAsync(
      Single<T> single, String operationName) {
    return single
        .doOnError(error -> log.error("Error in {}: {}", operationName, error.getMessage(), error))
        .onErrorResumeNext(throwable -> Single.error(mapToRestException(throwable, operationName)))
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }

  /**
   * Wraps a reactive Single operation with error handling and custom error mapping.
   *
   * @param single The reactive Single operation from service layer
   * @param operationName Name of the operation for logging purposes
   * @param errorMapper Custom function to map exceptions to RestException
   * @param <T> The type of data being returned
   * @return CompletionStage with ResponseEntity.Success or throws RestException on error
   */
  public static <T> CompletionStage<ResponseEntity.Success<T>> handleAsyncWithCustomError(
      Single<T> single, String operationName, Function<Throwable, RestException> errorMapper) {
    return single
        .doOnError(error -> log.error("Error in {}: {}", operationName, error.getMessage(), error))
        .onErrorResumeNext(throwable -> Single.error(errorMapper.apply(throwable)))
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
  }

  /**
   * Maps any exception to a RestException with appropriate HTTP status code.
   *
   * @param throwable The exception to map
   * @param operationName Name of the operation for context
   * @return RestException with appropriate status code and message
   */
  private static RestException mapToRestException(Throwable throwable, String operationName) {
    // If it's already a RestException, return as-is
    if (throwable instanceof RestException restException) {
      return restException;
    }

    // Map ConfigValidationException to BAD_REQUEST
    if (throwable instanceof ConfigValidationException configException) {
      return new RestException(
          configException.getErrorCode(),
          configException.getMessage(),
          HttpStatus.SC_BAD_REQUEST,
          configException);
    }

    // Map ConfigParsingException to BAD_REQUEST
    if (throwable instanceof ConfigParsingException parsingException) {
      return new RestException(
          parsingException.getErrorCode(),
          parsingException.getMessage(),
          HttpStatus.SC_BAD_REQUEST,
          parsingException);
    }

    // Map IllegalArgumentException to BAD_REQUEST
    if (throwable instanceof IllegalArgumentException) {
      return new RestException(
          "INVALID_ARGUMENT",
          "Invalid argument in " + operationName + ": " + throwable.getMessage(),
          HttpStatus.SC_BAD_REQUEST,
          throwable);
    }

    // Map NullPointerException to BAD_REQUEST (typically missing required field)
    if (throwable instanceof NullPointerException) {
      return new RestException(
          "MISSING_REQUIRED_FIELD",
          "Required field is missing in " + operationName,
          HttpStatus.SC_BAD_REQUEST,
          throwable);
    }

    // Map PostgreSQL-specific exceptions
    if (throwable.getClass().getName().contains("PgException")) {
      String errorMessage = throwable.getMessage();

      // Check for unique constraint violation (23505)
      if (errorMessage != null && errorMessage.contains("23505")) {
        return new RestException(
            "DUPLICATE_RESOURCE",
            "Resource already exists: " + errorMessage,
            HttpStatus.SC_CONFLICT,
            throwable);
      }

      // Check for foreign key violation (23503)
      if (errorMessage != null && errorMessage.contains("23503")) {
        return new RestException(
            "INVALID_REFERENCE",
            "Referenced resource does not exist: " + errorMessage,
            HttpStatus.SC_BAD_REQUEST,
            throwable);
      }

      // Check for not null violation (23502)
      if (errorMessage != null && errorMessage.contains("23502")) {
        return new RestException(
            "MISSING_REQUIRED_FIELD",
            "Required field is missing: " + errorMessage,
            HttpStatus.SC_BAD_REQUEST,
            throwable);
      }

      return new RestException(
          "DATABASE_ERROR",
          "Database operation failed in " + operationName + ": " + errorMessage,
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          throwable);
    }

    // Map common database/IO exceptions to INTERNAL_SERVER_ERROR
    if (throwable.getClass().getName().contains("SQLException")
        || throwable.getClass().getName().contains("IOException")) {
      return new RestException(
          "DATABASE_ERROR",
          "Database operation failed in " + operationName,
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          throwable);
    }

    // Default to INTERNAL_SERVER_ERROR
    return new RestException(
        "INTERNAL_ERROR",
        "Internal error occurred in " + operationName + ": " + throwable.getMessage(),
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        throwable);
  }

  /**
   * Creates a RestException for not found scenarios.
   *
   * @param resourceType Type of resource (e.g., "Audience", "Rule")
   * @param resourceId ID of the resource
   * @return RestException with 404 status
   */
  public static RestException notFoundException(String resourceType, Object resourceId) {
    return new RestException(
        "RESOURCE_NOT_FOUND",
        String.format("%s with ID %s not found", resourceType, resourceId),
        HttpStatus.SC_NOT_FOUND);
  }

  /**
   * Creates a RestException for bad request scenarios.
   *
   * @param message Error message
   * @return RestException with 400 status
   */
  public static RestException badRequestException(String message) {
    return new RestException("BAD_REQUEST", message, HttpStatus.SC_BAD_REQUEST);
  }

  /**
   * Creates a RestException for conflict scenarios (e.g., duplicate resources).
   *
   * @param message Error message
   * @return RestException with 409 status
   */
  public static RestException conflictException(String message) {
    return new RestException("CONFLICT", message, HttpStatus.SC_CONFLICT);
  }

  /**
   * Creates a RestException for service unavailable scenarios.
   *
   * @param serviceName Name of the unavailable service
   * @return RestException with 503 status
   */
  public static RestException serviceUnavailableException(String serviceName) {
    return new RestException(
        "SERVICE_UNAVAILABLE",
        String.format("%s is currently unavailable", serviceName),
        HttpStatus.SC_SERVICE_UNAVAILABLE);
  }
}
