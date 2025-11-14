package com.ascend.flockr.exception;

import com.dream11.rest.exception.RestError;
import com.dream11.rest.exception.RestException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorEnum implements RestError {
  REST_HEALTH_CHECK_FAILED(
      "flockr_REST_HEALTH_CHECK_FAILED",
      "HealthCheck Failed for flockr service",
      HttpStatus.SC_INTERNAL_SERVER_ERROR),

  AUDIENCE_NOT_FOUND("AUDIENCE_NOT_FOUND", "Audience not found", HttpStatus.SC_NOT_FOUND),

  RULE_NOT_FOUND("RULE_NOT_FOUND", "Rule not found", HttpStatus.SC_NOT_FOUND),

  INVALID_REQUEST_BODY(
      "INVALID_REQUEST_BODY", "Invalid request body parameters", HttpStatus.SC_BAD_REQUEST),

  DATABASE_ERROR(
      "DATABASE_ERROR", "Database operation failed", HttpStatus.SC_INTERNAL_SERVER_ERROR),

  INTERNAL_ERROR(
      "INTERNAL_ERROR",
      "An unexpected internal error occurred",
      HttpStatus.SC_INTERNAL_SERVER_ERROR);

  private final String errorCode;
  private final String errorMessage;
  private final int httpStatusCode;

  public static RestException handleException(Throwable throwable, RestException defaultException) {
    if (throwable instanceof RestException restException) return restException;
    else return defaultException;
  }
}
