package io.ascend.flockr.admin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a String value matches one of the values in the specified enum.
 *
 * <p>This annotation can be used on String fields to ensure the value is a valid enum constant
 * name. The validation is case-sensitive.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @ValidEnum(enumClass = AudienceType.class, message = "Invalid audience type")
 * private String type;
 * }</pre>
 *
 * @author Flockr Team
 * @since 1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEnumValidator.class)
@Documented
public @interface ValidEnum {
  /**
   * The enum class to validate against.
   *
   * @return the enum class
   */
  Class<? extends Enum<?>> enumClass();

  /**
   * The error message to display when validation fails.
   *
   * @return the error message
   */
  String message() default "Value must be one of the allowed enum values";

  /**
   * Validation groups.
   *
   * @return the groups
   */
  Class<?>[] groups() default {};

  /**
   * Payload for clients.
   *
   * @return the payload
   */
  Class<? extends Payload>[] payload() default {};
}
