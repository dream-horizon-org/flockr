package com.ascend.flockr.users.validator;

import com.ascend.flockr.users.constants.Constants;
import com.ascend.flockr.users.dto.request.BatchMapUserCohortsRequest;
import com.ascend.flockr.users.exception.errors.DefinedErrors;
import com.dream11.rest.util.ExceptionUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Validator for batch cohort mapping requests.
 *
 * <p>Validates that the batch list and each individual request are valid.
 *
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class BatchMapUserCohortsRequestValidator {

  /**
   * Validates a batch of mapping requests.
   *
   * @param requests the list of requests to validate
   * @throws RuntimeException if validation fails
   */
  public static void validate(List<BatchMapUserCohortsRequest> requests) {
    validateBatchList(requests);
    validateEachRequest(requests);
  }

  /**
   * Validates that the batch list is not null or empty.
   */
  private static void validateBatchList(List<BatchMapUserCohortsRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      log.error("Batch request list is null or empty");
      throw ExceptionUtil.getException(
          DefinedErrors.INVALID_REQUEST,
          "Batch request list cannot be empty");
    }
  }

  /**
   * Validates each request in the batch.
   */
  private static void validateEachRequest(List<BatchMapUserCohortsRequest> requests) {
    for (int index = 0; index < requests.size(); index++) {
      BatchMapUserCohortsRequest request = requests.get(index);
      validateRequest(request, index);
    }
  }

  /**
   * Validates a single request in the batch.
   */
  private static void validateRequest(BatchMapUserCohortsRequest request, int index) {
    if (request == null) {
      throwInvalidRequestError("Request at index " + index + " is null", index);
    }

    validateUserId(request, index);
    validateCohortKey(request, index);
    validateAction(request, index);
    validateExpireAt(request, index);
  }

  /**
   * Validates the user ID field.
   */
  private static void validateUserId(BatchMapUserCohortsRequest request, int index) {
    if (request.getUserId() == null || request.getUserId() <= 0) {
      log.error("Invalid user_id at index {}: {}", index, request.getUserId());
      throw ExceptionUtil.getException(
          DefinedErrors.INVALID_USER_ID,
          String.valueOf(request.getUserId()));
    }
  }

  /**
   * Validates the cohort key field.
   */
  private static void validateCohortKey(BatchMapUserCohortsRequest request, int index) {
    if (isBlank(request.getCohortKey())) {
      log.error("Missing cohort_key at index {}", index);
      throw ExceptionUtil.getException(DefinedErrors.MISSING_COHORT_KEY);
    }
  }

  /**
   * Validates the action field.
   */
  private static void validateAction(BatchMapUserCohortsRequest request, int index) {
    if (isBlank(request.getAction())) {
      log.error("Missing action at index {}", index);
      throw ExceptionUtil.getException(DefinedErrors.MISSING_ACTION);
    }

    if (!isValidAction(request.getAction())) {
      log.error("Invalid action value at index {}: {}", index, request.getAction());
      throw ExceptionUtil.getException(DefinedErrors.INVALID_ACTION, request.getAction());
    }
  }

  /**
   * Validates the expire_at field.
   */
  private static void validateExpireAt(BatchMapUserCohortsRequest request, int index) {
    if (isBlank(request.getExpireAt())) {
      log.error("Missing expire_at at index {}", index);
      throw ExceptionUtil.getException(DefinedErrors.MISSING_EXPIRE_AT);
    }
  }

  /**
   * Checks if a string is null or blank.
   */
  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  /**
   * Checks if the action is valid (either append or remove).
   */
  private static boolean isValidAction(String action) {
    return Constants.ACTION_APPEND.equals(action) || Constants.ACTION_REMOVE.equals(action);
  }

  /**
   * Throws an invalid request error with a descriptive message.
   */
  private static void throwInvalidRequestError(String message, int index) {
    log.error("Validation error at index {}: {}", index, message);
    throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST, message);
  }
}

