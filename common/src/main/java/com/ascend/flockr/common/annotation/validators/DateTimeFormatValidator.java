package com.ascend.flockr.common.annotation.validators;

import com.ascend.flockr.common.annotation.DateTimeFormat;


import com.ascend.flockr.common.utils.CommonUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link DateTimeFormat} annotation.
 *
 * <p>This validator checks that a string value matches the date-time format defined in {@link
 * com.ascend.flockr.common.constants.Constants#DATE_PATTERN} ("yyyy-MM-dd HH:mm:ss"). It uses
 * the {@link com.ascend.flockr.common.utils.CommonUtils#getFormatter()} to parse the value.
 *
 * <p><strong>Validation Rules:</strong>
 *
 * <ul>
 *   <li>Null values are considered valid (validation passes)
 *   <li>Non-null values must parse successfully using the standard formatter
 *   <li>If parsing throws any exception, validation fails
 * </ul>
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * // Valid values
 * "2024-01-15 14:30:00"  // passes
 * null                    // passes
 *
 * // Invalid values
 * "2024-01-15"           // fails (missing time)
 * "15/01/2024"           // fails (wrong format)
 * "invalid-date"         // fails (cannot parse)
 * }</pre>
 *
 * @author Flockr Team
 * @since 1.0
 * @see DateTimeFormat
 * @see com.ascend.flockr.common.utils.CommonUtils#getFormatter()
 */
public class DateTimeFormatValidator implements ConstraintValidator<DateTimeFormat, String> {

  /**
   * Validates that the string value matches the standard date-time format.
   *
   * <p>This method attempts to parse the value using {@link
   * com.ascend.flockr.common.utils.CommonUtils#getFormatter()}. If parsing succeeds, the value is
   * valid. If parsing throws an exception, the value is invalid.
   *
   * @param value the string value to validate, may be null
   * @param context the constraint validator context (unused)
   * @return {@code true} if the value is null or successfully parses, {@code false} otherwise
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    try {
      CommonUtils.getFormatter().parse(value);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
