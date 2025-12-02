package com.ascend.flockr.users.annotation;

import com.ascend.flockr.users.annotation.validators.DateTimeFormatValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation to ensure a string value matches the standard date-time format.
 *
 * <p>This annotation validates that a string parameter or field follows the date-time pattern
 * defined in {@link com.ascend.flockr.users.constants.Constants#DATE_PATTERN} ("yyyy-MM-dd
 * HH:mm:ss"). The validation is performed by {@link DateTimeFormatValidator}.
 *
 * <p><strong>Format:</strong> {@code "yyyy-MM-dd HH:mm:ss"}
 *
 * <p><strong>Examples:</strong>
 *
 * <ul>
 *   <li>Valid: {@code "2024-01-15 14:30:00"}
 *   <li>Invalid: {@code "2024-01-15"} (missing time)
 *   <li>Invalid: {@code "15/01/2024 14:30:00"} (wrong format)
 * </ul>
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * public void setExpiryDate(@DateTimeFormat String expireAt) {
 *   // expireAt will be validated to match "yyyy-MM-dd HH:mm:ss" format
 * }
 * }</pre>
 *
 * <p><strong>Null Handling:</strong>
 *
 * <p>Null values are considered valid (validation passes). To require non-null values, combine with
 * {@code @NotNull}.
 *
 * @author Flockr Team
 * @since 1.0
 * @see DateTimeFormatValidator
 * @see com.ascend.flockr.users.constants.Constants#DATE_PATTERN
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Constraint(validatedBy = DateTimeFormatValidator.class)
public @interface DateTimeFormat {
  /**
   * Default error message when validation fails.
   *
   * @return the error message
   */
  String message() default "invalid datetime format value";

  /**
   * Validation groups (unused in current implementation).
   *
   * @return validation groups
   */
  Class<?>[] groups() default {};

  /**
   * Payload for metadata (unused in current implementation).
   *
   * @return payload classes
   */
  Class<? extends Payload>[] payload() default {};
}

