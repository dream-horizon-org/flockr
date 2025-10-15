package com.ascend.flockr.common.exception.errors;

import com.dream11.rest.exception.RestError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DefinedErrors implements RestError {
  INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "unknown error occurred due to %s", 500),

  INVALID_REQUEST("INVALID_REQUEST", "The request violates one or more constraints: %s", 400),

  INVALID_REQUEST_PARAMS("INVALID_REQUEST_PARAMS", "Invalid request params", 400),

  INVALID_EXPIRY_TIME("INVALID_EXPIRY_TIME", "Invalid expiry time %s", 400),

  AEROSPIKE_APPEND_FAILED("AEROSPIKE_APPEND_FAILED", "append to aerospike failed", 500);

  private final String errorCode;
  private final String errorMessage;
  private final int httpStatusCode;
}
