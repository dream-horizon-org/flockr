package io.ascend.flockr.admin.provider;

import com.dream11.rest.exception.RestException;
import io.ascend.flockr.admin.exception.ConfigParsingException;
import io.ascend.flockr.admin.exception.ConfigValidationException;
import io.ascend.flockr.admin.io.ResponseEntity;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

/**
 * Generic exception mapper that handles all uncaught exceptions in REST endpoints. This provides a
 * centralized error handling mechanism for all API flows.
 */
@Slf4j
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

  @Override
  public Response toResponse(Throwable throwable) {
    log.error("Exception occurred in REST API", throwable);

    // Handle RestException
    if (throwable instanceof RestException restException) {
      return buildErrorResponse(
          restException.getErrorCode(),
          restException.getMessage(),
          getCauseMessage(restException),
          restException.getHttpStatusCode());
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

    // Handle ConfigValidationException
    if (throwable instanceof ConfigValidationException configException) {
      return buildErrorResponse(
          configException.getErrorCode(),
          configException.getMessage(),
          getCauseMessage(configException),
          HttpStatus.SC_BAD_REQUEST);
    }

    // Handle ConfigParsingException
    if (throwable instanceof ConfigParsingException parsingException) {
      return buildErrorResponse(
          parsingException.getErrorCode(),
          parsingException.getMessage(),
          getCauseMessage(parsingException),
          HttpStatus.SC_BAD_REQUEST);
    }

    // Handle IllegalArgumentException (typically validation errors)
    if (throwable instanceof IllegalArgumentException) {
      return buildErrorResponse(
          "INVALID_ARGUMENT",
          "Invalid request parameters",
          throwable.getMessage(),
          HttpStatus.SC_BAD_REQUEST);
    }

    // Handle NullPointerException
    if (throwable instanceof NullPointerException) {
      return buildErrorResponse(
          "NULL_POINTER_ERROR",
          "Required value is missing",
          throwable.getMessage(),
          HttpStatus.SC_BAD_REQUEST);
    }

    // Default: Internal Server Error
    return buildErrorResponse(
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred",
        throwable.getMessage(),
        HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  /** Build error response with consistent structure. */
  private Response buildErrorResponse(String code, String message, String cause, int statusCode) {
    ResponseEntity.Failure failure =
        new ResponseEntity.Failure(
            code, message, cause != null ? cause : "No additional details available");

    return Response.status(statusCode).entity(failure).type(MediaType.APPLICATION_JSON).build();
  }

  /** Extract cause message from exception chain. */
  private String getCauseMessage(Throwable throwable) {
    if (throwable.getCause() != null) {
      return throwable.getCause().getMessage();
    }
    return throwable.getMessage();
  }

  /** Get appropriate message for WebApplicationException status codes. */
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
