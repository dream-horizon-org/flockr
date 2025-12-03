package com.ascend.flockr.users.validator;

import com.ascend.flockr.users.exception.errors.DefinedErrors;
import com.dream11.rest.util.ExceptionUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for validating HTTP headers.
 *
 * <p>This validator provides methods to validate header presence and basic format requirements. It
 * does not perform format validation (e.g., UUID format, project key format) - those are handled by
 * upstream services.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class HeaderValidator {

  /**
   * Validates that the userId header is present and not empty.
   *
   * @param userIdHeader the userId header value, may be null or empty
   * @throws RuntimeException if userId header is missing or empty
   */
  public static void validateUserIdHeader(String userIdHeader) {
    if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
      log.error("Missing userId header");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_USER_ID_HEADER);
    }
  }

  /**
   * Validates that the x-project-key header is present and not empty.
   *
   * @param projectKey the x-project-key header value, may be null or empty
   * @throws RuntimeException if x-project-key header is missing or empty
   */
  public static void validateProjectKeyHeader(String projectKey) {
    if (projectKey == null || projectKey.trim().isEmpty()) {
      log.error("Missing x-project-key header");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_PROJECT_KEY_HEADER);
    }
  }

  /**
   * Validates that userId header is present and parses it to a Long.
   *
   * <p>Only validates presence. No format or value validation is performed.
   *
   * @param userIdHeader the userId header value
   * @return the parsed userId as a Long
   * @throws RuntimeException if userId is missing
   * @throws NumberFormatException if userId cannot be parsed as Long
   */
  public static Long validateAndParseUserId(String userIdHeader) {
    validateUserIdHeader(userIdHeader);
    return Long.parseLong(userIdHeader.trim());
  }
}
