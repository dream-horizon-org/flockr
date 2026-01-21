package io.ascend.flockr.admin.provider;

import com.dream11.rest.exception.RestException;
import io.ascend.flockr.admin.exception.ConfigParsingException;
import io.ascend.flockr.admin.exception.ConfigValidationException;
import io.ascend.flockr.admin.exception.FlinkClientException;
import io.ascend.flockr.admin.exception.ForbiddenAccessException;
import io.ascend.flockr.admin.exception.ResourceNotFoundException;
import io.ascend.flockr.admin.io.ResponseEntity;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

/**
 * Generic exception mapper that handles all uncaught exceptions in REST endpoints.
 *
 * <p>This provider implements a centralized error handling mechanism for all API flows, ensuring
 * consistent error responses across the module. The mapper handles the following exception types:
 *
 * <ul>
 *   <li>{@link RestException} - Custom REST exceptions with specific error codes and status
 *   <li>{@link ResourceNotFoundException} - 404 Not Found for missing resources
 *   <li>{@link ForbiddenAccessException} - 403 Forbidden for unauthorized access
 *   <li>{@link NoSuchElementException} - 404 Not Found (typically from database queries)
 *   <li>{@link IllegalAccessException} - 403 Forbidden
 *   <li>{@link FlinkClientException} - Flink client errors (status varies)
 *   <li>{@link ConfigValidationException} - 400 Bad Request for invalid configurations
 *   <li>{@link ConfigParsingException} - 400 Bad Request for parsing errors
 *   <li>{@link WebApplicationException} - JAX-RS specific exceptions
 *   <li>{@link IllegalArgumentException} - 400 Bad Request for invalid arguments
 *   <li>{@link IllegalStateException} - 400/409 based on context
 *   <li>Other exceptions - 500 Internal Server Error
 * </ul>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

  @Override
  public Response toResponse(Throwable throwable) {
    log.error("Exception occurred in REST API", throwable);

    // Handle RestException (already formatted for REST)
    if (throwable instanceof RestException restException) {
      return buildErrorResponse(
          restException.getErrorCode(),
          restException.getMessage(),
          getCauseMessage(restException),
          restException.getHttpStatusCode());
    }

    // Handle ResourceNotFoundException (404)
    if (throwable instanceof ResourceNotFoundException notFoundException) {
      return buildErrorResponse(
          notFoundException.getErrorCode(),
          notFoundException.getMessage(),
          getCauseMessage(notFoundException),
          HttpStatus.SC_NOT_FOUND);
    }

    // Handle ForbiddenAccessException (403)
    if (throwable instanceof ForbiddenAccessException forbiddenException) {
      return buildErrorResponse(
          forbiddenException.getErrorCode(),
          forbiddenException.getMessage(),
          getCauseMessage(forbiddenException),
          HttpStatus.SC_FORBIDDEN);
    }

    // Handle NoSuchElementException (404) - typically from database fetchOne
    if (throwable instanceof NoSuchElementException) {
      return buildErrorResponse(
          "RESOURCE_NOT_FOUND",
          "Requested resource not found",
          throwable.getMessage(),
          HttpStatus.SC_NOT_FOUND);
    }

    // Handle IllegalAccessException (403)
    if (throwable instanceof IllegalAccessException) {
      return buildErrorResponse(
          "FORBIDDEN", "Access denied", throwable.getMessage(), HttpStatus.SC_FORBIDDEN);
    }

    // Handle FlinkClientException (uses its own status code)
    if (throwable instanceof FlinkClientException flinkException) {
      return buildErrorResponse(
          flinkException.getErrorCode(),
          flinkException.getMessage(),
          getCauseMessage(flinkException),
          flinkException.getStatusCode());
    }

    // Handle JAX-RS WebApplicationException
    if (throwable instanceof WebApplicationException webAppException) {
      int statusCode = webAppException.getResponse().getStatus();
      return buildErrorResponse(
          "WEB_APPLICATION_ERROR",
          getWebApplicationMessage(statusCode),
          throwable.getMessage(),
          statusCode);
    }

    // Handle ConfigValidationException (400)
    if (throwable instanceof ConfigValidationException configException) {
      return buildErrorResponse(
          configException.getErrorCode(),
          configException.getMessage(),
          getCauseMessage(configException),
          HttpStatus.SC_BAD_REQUEST);
    }

    // Handle ConfigParsingException (400)
    if (throwable instanceof ConfigParsingException parsingException) {
      return buildErrorResponse(
          parsingException.getErrorCode(),
          parsingException.getMessage(),
          getCauseMessage(parsingException),
          HttpStatus.SC_BAD_REQUEST);
    }

    // Handle IllegalArgumentException (400)
    if (throwable instanceof IllegalArgumentException) {
      return buildErrorResponse(
          "INVALID_ARGUMENT",
          "Invalid request parameters",
          throwable.getMessage(),
          HttpStatus.SC_BAD_REQUEST);
    }

    // Handle IllegalStateException (400/409 based on context)
    if (throwable instanceof IllegalStateException) {
      String message = throwable.getMessage();
      if (message != null
          && (message.contains("already")
              || message.contains("duplicate")
              || message.contains("exists"))) {
        return buildErrorResponse(
            "CONFLICT", message, getCauseMessage(throwable), HttpStatus.SC_CONFLICT);
      }
      return buildErrorResponse(
          "INVALID_STATE",
          "Invalid operation state",
          throwable.getMessage(),
          HttpStatus.SC_BAD_REQUEST);
    }

    // Handle NullPointerException (400)
    if (throwable instanceof NullPointerException) {
      return buildErrorResponse(
          "NULL_POINTER_ERROR",
          "Required value is missing",
          throwable.getMessage(),
          HttpStatus.SC_BAD_REQUEST);
    }

    // Default: Internal Server Error (500)
    return buildErrorResponse(
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred",
        throwable.getMessage(),
        HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  /**
   * Builds a standardized error response with consistent structure.
   *
   * @param code the error code
   * @param message the user-friendly error message
   * @param cause additional cause information
   * @param statusCode the HTTP status code
   * @return a Response object with the error entity
   */
  private Response buildErrorResponse(String code, String message, String cause, int statusCode) {
    ResponseEntity.Failure failure =
        new ResponseEntity.Failure(
            code, message, cause != null ? cause : "No additional details available");

    return Response.status(statusCode).entity(failure).type(MediaType.APPLICATION_JSON).build();
  }

  /**
   * Extracts the cause message from the exception chain.
   *
   * @param throwable the exception to extract cause from
   * @return the cause message or the original message if no cause exists
   */
  private String getCauseMessage(Throwable throwable) {
    if (throwable.getCause() != null) {
      return throwable.getCause().getMessage();
    }
    return throwable.getMessage();
  }

  /**
   * Gets an appropriate user-friendly message for WebApplicationException status codes.
   *
   * @param statusCode the HTTP status code
   * @return a human-readable message
   */
  private String getWebApplicationMessage(int statusCode) {
    return switch (statusCode) {
      case 400 -> "Bad Request - Invalid input provided";
      case 401 -> "Unauthorized - Authentication required";
      case 403 -> "Forbidden - Access denied";
      case 404 -> "Not Found - Resource does not exist";
      case 405 -> "Method Not Allowed";
      case 409 -> "Conflict - Resource already exists";
      case 422 -> "Unprocessable Entity - Validation failed";
      case 500 -> "Internal Server Error";
      case 502 -> "Bad Gateway";
      case 503 -> "Service Unavailable";
      case 504 -> "Gateway Timeout";
      default -> "Request failed with status " + statusCode;
    };
  }
}
