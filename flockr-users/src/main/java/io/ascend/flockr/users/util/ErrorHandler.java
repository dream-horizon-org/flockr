package io.ascend.flockr.users.util;

import com.dream11.rest.exception.RestException;
import io.ascend.flockr.users.dto.ResponseEntity;
import io.ascend.flockr.users.exception.DefinedException;
import io.reactivex.rxjava3.core.Single;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

/**
 * Utility class for handling errors in REST API flows. Provides generic error handling that can be
 * reused across all controllers.
 *
 * <p>This class provides a centralized error handling mechanism that maps various exception types
 * to appropriate HTTP status codes and error responses:
 *
 * <ul>
 *   <li>{@link DefinedException} → 400 Bad Request (application-defined errors)
 *   <li>{@link NoSuchElementException} → 404 Not Found (typically from database fetch operations)
 *   <li>{@link IllegalArgumentException} → 400 Bad Request
 *   <li>{@link IllegalStateException} → 400 Bad Request or 409 Conflict
 *   <li>Aerospike exceptions → 500 Internal Server Error or specific error codes
 *   <li>Other exceptions → 500 Internal Server Error
 * </ul>
 *
 * @author Prithu Sharma
 * @since 1.0
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
   * <p>This method provides a standardized mapping from various exception types to REST-friendly
   * error responses. The mapping is as follows:
   *
   * <ul>
   *   <li>{@link RestException} - returned as-is
   *   <li>{@link DefinedException} - 400 Bad Request
   *   <li>{@link NoSuchElementException} - 404 Not Found (database fetch returned no rows)
   *   <li>{@link IllegalArgumentException} - 400 Bad Request
   *   <li>{@link IllegalStateException} - 400 Bad Request or 409 Conflict
   *   <li>{@link NullPointerException} - 400 Bad Request (missing required field)
   *   <li>Aerospike exceptions - mapped based on error type
   *   <li>Other exceptions - 500 Internal Server Error
   * </ul>
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

    // Map DefinedException to BAD_REQUEST
    if (throwable instanceof DefinedException definedException) {
      return new RestException(
          "DEFINED_ERROR",
          definedException.getMessage(),
          HttpStatus.SC_BAD_REQUEST,
          definedException);
    }

    // Map NoSuchElementException to NOT_FOUND (typically from database fetchOne operations)
    if (throwable instanceof NoSuchElementException) {
      return new RestException(
          "RESOURCE_NOT_FOUND",
          "Requested resource not found in " + operationName,
          HttpStatus.SC_NOT_FOUND,
          throwable);
    }

    // Map IllegalArgumentException to BAD_REQUEST
    if (throwable instanceof IllegalArgumentException) {
      return new RestException(
          "INVALID_ARGUMENT",
          "Invalid argument in " + operationName + ": " + throwable.getMessage(),
          HttpStatus.SC_BAD_REQUEST,
          throwable);
    }

    // Map IllegalStateException to CONFLICT or BAD_REQUEST based on context
    if (throwable instanceof IllegalStateException) {
      String message = throwable.getMessage();
      // Check for common patterns that indicate a conflict state
      if (message != null
          && (message.contains("already")
              || message.contains("duplicate")
              || message.contains("exists"))) {
        return new RestException("CONFLICT", message, HttpStatus.SC_CONFLICT, throwable);
      }
      return new RestException(
          "INVALID_STATE",
          "Invalid state in " + operationName + ": " + message,
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

    // Map Aerospike-specific exceptions
    if (throwable.getClass().getName().contains("AerospikeException")) {
      return mapAerospikeException(throwable, operationName);
    }

    // Map common IO exceptions to INTERNAL_SERVER_ERROR
    if (throwable.getClass().getName().contains("IOException")) {
      return new RestException(
          "IO_ERROR",
          "IO operation failed in " + operationName,
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
   * Maps Aerospike-specific exceptions to appropriate RestExceptions.
   *
   * @param throwable the Aerospike exception
   * @param operationName the name of the operation for context
   * @return a RestException with the appropriate status code
   */
  private static RestException mapAerospikeException(Throwable throwable, String operationName) {
    String errorMessage = throwable.getMessage();

    // Check for timeout errors
    if (errorMessage != null && errorMessage.toLowerCase().contains("timeout")) {
      return new RestException(
          "AEROSPIKE_TIMEOUT",
          "Aerospike operation timed out in " + operationName,
          HttpStatus.SC_GATEWAY_TIMEOUT,
          throwable);
    }

    // Check for connection errors
    if (errorMessage != null
        && (errorMessage.toLowerCase().contains("connection")
            || errorMessage.toLowerCase().contains("unavailable"))) {
      return new RestException(
          "AEROSPIKE_UNAVAILABLE",
          "Aerospike is currently unavailable",
          HttpStatus.SC_SERVICE_UNAVAILABLE,
          throwable);
    }

    // Check for key not found
    if (errorMessage != null && errorMessage.toLowerCase().contains("key not found")) {
      return new RestException(
          "RESOURCE_NOT_FOUND",
          "Record not found in Aerospike",
          HttpStatus.SC_NOT_FOUND,
          throwable);
    }

    // Default Aerospike error
    return new RestException(
        "AEROSPIKE_ERROR",
        "Aerospike operation failed in " + operationName + ": " + errorMessage,
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        throwable);
  }

  /**
   * Creates a RestException for not found scenarios.
   *
   * @param resourceType Type of resource (e.g., "User", "Cohort")
   * @param resourceId ID of the resource
   * @return RestException with 404 status
   */
  public static RestException notFoundException(String resourceType, Object resourceId) {
    return new RestException(
        resourceType.toUpperCase() + "_NOT_FOUND",
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
