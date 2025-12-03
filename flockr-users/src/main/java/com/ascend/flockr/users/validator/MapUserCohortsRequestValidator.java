package com.ascend.flockr.users.validator;

import com.ascend.flockr.users.constants.Constants;
import com.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import com.ascend.flockr.users.exception.errors.DefinedErrors;
import com.dream11.rest.util.ExceptionUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Validator for MapUserCohortsRequest DTO.
 *
 * <p>Validates that all required fields are present and have valid values. This validator performs
 * presence checks and basic value validation. Format validation (e.g., date format) is handled by
 * Bean Validation annotations on the DTO.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class MapUserCohortsRequestValidator {

  /**
   * Validates a MapUserCohortsRequest object.
   *
   * <p>Validates:
   *
   * <ul>
   *   <li>Request is not null
   *   <li>cohort_key is present and not empty
   *   <li>action is present and not empty
   *   <li>expire_at is present and not empty
   *   <li>action value is either "append" or "remove"
   *   <li>Bean Validation constraints (via request.validate())
   * </ul>
   *
   * @param request the request to validate, may be null
   * @throws RuntimeException if validation fails
   */
  public static void validate(MapUserCohortsRequest request) {
    // Validate request body is not null
    if (request == null) {
      log.error("Request body is null");
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST, "Request body is required");
    }

    // Validate required fields
    if (request.getCohortKey() == null || request.getCohortKey().trim().isEmpty()) {
      log.error("Missing cohort_key in request body");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_COHORT_KEY);
    }

    if (request.getAction() == null || request.getAction().trim().isEmpty()) {
      log.error("Missing action in request body");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_ACTION);
    }

    if (request.getExpireAt() == null || request.getExpireAt().trim().isEmpty()) {
      log.error("Missing expire_at in request body");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_EXPIRE_AT);
    }

    // Validate action value
    if (!request.getAction().equals(Constants.ACTION_APPEND)
        && !request.getAction().equals(Constants.ACTION_REMOVE)) {
      log.error("Invalid action value: {}", request.getAction());
      throw ExceptionUtil.getException(DefinedErrors.INVALID_ACTION, request.getAction());
    }

    // Validate request body using Bean Validation
    request.validate();
  }
}
