package io.ascend.flockr.admin.util;

import com.dream11.rest.exception.RestException;
import io.ascend.flockr.admin.client.flink.FlinkClientException;
import io.ascend.flockr.admin.exception.ConfigParsingException;
import io.ascend.flockr.admin.exception.ConfigValidationException;
import io.ascend.flockr.admin.exception.ForbiddenAccessException;
import io.ascend.flockr.admin.exception.ResourceNotFoundException;
import io.ascend.flockr.admin.io.ResponseEntity;
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
 *   <li>{@link ResourceNotFoundException} → 404 Not Found
 *   <li>{@link ForbiddenAccessException} → 403 Forbidden
 *   <li>{@link NoSuchElementException} → 404 Not Found (typically from database fetch operations)
 *   <li>{@link IllegalAccessException} → 403 Forbidden
 *   <li>{@link ConfigValidationException} → 400 Bad Request
 *   <li>{@link ConfigParsingException} → 400 Bad Request
 *   <li>{@link IllegalArgumentException} → 400 Bad Request
 *   <li>Database exceptions → 500 Internal Server Error or specific error codes
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
   *   <li>{@link ResourceNotFoundException} - 404 Not Found
   *   <li>{@link ForbiddenAccessException} - 403 Forbidden
   *   <li>{@link NoSuchElementException} - 404 Not Found (database fetch returned no rows)
   *   <li>{@link IllegalAccessException} - 403 Forbidden
   *   <li>{@link ConfigValidationException} - 400 Bad Request
   *   <li>{@link ConfigParsingException} - 400 Bad Request
   *   <li>{@link IllegalArgumentException} - 400 Bad Request
   *   <li>{@link NullPointerException} - 400 Bad Request (missing required field)
   *   <li>PostgreSQL exceptions - mapped based on error code
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

    // Map ResourceNotFoundException to NOT_FOUND
    if (throwable instanceof ResourceNotFoundException notFoundException) {
      return new RestException(
          notFoundException.getErrorCode(),
          notFoundException.getMessage(),
          HttpStatus.SC_NOT_FOUND,
          notFoundException);
    }

    // Map ForbiddenAccessException to FORBIDDEN
    if (throwable instanceof ForbiddenAccessException forbiddenException) {
      return new RestException(
          forbiddenException.getErrorCode(),
          forbiddenException.getMessage(),
          HttpStatus.SC_FORBIDDEN,
          forbiddenException);
    }

    // Map NoSuchElementException to NOT_FOUND (typically from database fetchOne operations)
    if (throwable instanceof NoSuchElementException) {
      return new RestException(
          "RESOURCE_NOT_FOUND",
          "Requested resource not found in " + operationName,
          HttpStatus.SC_NOT_FOUND,
          throwable);
    }

    // Map IllegalAccessException to FORBIDDEN
    if (throwable instanceof IllegalAccessException) {
      return new RestException(
          "FORBIDDEN",
          "Access denied: " + throwable.getMessage(),
          HttpStatus.SC_FORBIDDEN,
          throwable);
    }

    // Map FlinkClientException to appropriate status based on its statusCode
    if (throwable instanceof FlinkClientException flinkException) {
      return new RestException(
          flinkException.getErrorCode(),
          flinkException.getMessage(),
          flinkException.getStatusCode(),
          flinkException);
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

    // Map PostgreSQL-specific exceptions
    if (throwable.getClass().getName().contains("PgException")) {
      return mapPostgresException(throwable, operationName);
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
   * Maps PostgreSQL-specific exceptions to appropriate RestExceptions.
   *
   * @param throwable the PostgreSQL exception
   * @param operationName the name of the operation for context
   * @return a RestException with the appropriate status code
   */
  private static RestException mapPostgresException(Throwable throwable, String operationName) {
    String errorMessage = throwable.getMessage();

    // Check for unique constraint violation (23505)
    if (errorMessage != null && errorMessage.contains("23505")) {
      return new RestException(
          "DUPLICATE_RESOURCE",
          "Resource already exists: " + extractConstraintMessage(errorMessage),
          HttpStatus.SC_CONFLICT,
          throwable);
    }

    // Check for foreign key violation (23503)
    if (errorMessage != null && errorMessage.contains("23503")) {
      return new RestException(
          "INVALID_REFERENCE",
          "Referenced resource does not exist: " + extractConstraintMessage(errorMessage),
          HttpStatus.SC_BAD_REQUEST,
          throwable);
    }

    // Check for not null violation (23502)
    if (errorMessage != null && errorMessage.contains("23502")) {
      return new RestException(
          "MISSING_REQUIRED_FIELD",
          "Required field is missing: " + extractConstraintMessage(errorMessage),
          HttpStatus.SC_BAD_REQUEST,
          throwable);
    }

    // Check for check constraint violation (23514)
    if (errorMessage != null && errorMessage.contains("23514")) {
      return new RestException(
          "CONSTRAINT_VIOLATION",
          "Data validation failed: " + extractConstraintMessage(errorMessage),
          HttpStatus.SC_BAD_REQUEST,
          throwable);
    }

    return new RestException(
        "DATABASE_ERROR",
        "Database operation failed in " + operationName + ": " + errorMessage,
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        throwable);
  }

  /**
   * Extracts a user-friendly message from PostgreSQL constraint error messages.
   *
   * @param errorMessage the raw error message
   * @return a cleaner message for API responses
   */
  private static String extractConstraintMessage(String errorMessage) {
    // PostgreSQL error messages often contain "Detail:" with useful info
    if (errorMessage.contains("Detail:")) {
      int detailStart = errorMessage.indexOf("Detail:");
      int detailEnd = errorMessage.indexOf("\n", detailStart);
      if (detailEnd == -1) {
        return errorMessage.substring(detailStart + 8).trim();
      }
      return errorMessage.substring(detailStart + 8, detailEnd).trim();
    }
    return errorMessage;
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
        resourceType.toUpperCase() + "_NOT_FOUND",
        String.format("%s with ID %s not found", resourceType, resourceId),
        HttpStatus.SC_NOT_FOUND);
  }

  /**
   * Creates a RestException for forbidden access scenarios.
   *
   * @param message Error message describing why access is forbidden
   * @return RestException with 403 status
   */
  public static RestException forbiddenException(String message) {
    return new RestException("FORBIDDEN", message, HttpStatus.SC_FORBIDDEN);
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
