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

  TASK_NOT_FOUND("flockr_TASK_NOT_FOUND", "Task not found", HttpStatus.SC_NOT_FOUND),

  TASK_NOT_TRIGGERABLE(
      "flockr_TASK_NOT_TRIGGERABLE",
      "Task cannot be triggered in its current state",
      HttpStatus.SC_BAD_REQUEST),

  TASK_TRIGGER_FAILED(
      "flockr_TASK_TRIGGER_FAILED", "Failed to trigger task", HttpStatus.SC_INTERNAL_SERVER_ERROR),

  UNAUTHORIZED("flockr_UNAUTHORIZED", "Unauthorized access", HttpStatus.SC_UNAUTHORIZED);

  private final String errorCode;
  private final String errorMessage;
  private final int httpStatusCode;

  public static RestException handleException(Throwable throwable, RestException defaultException) {
    if (throwable instanceof RestException restException) return restException;
    else return defaultException;
  }
}
