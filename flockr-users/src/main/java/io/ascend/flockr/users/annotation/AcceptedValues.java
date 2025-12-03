package io.ascend.flockr.users.annotation;

import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation to ensure a string value is one of the accepted values.
 *
 * <p>This annotation can be applied to method parameters and fields to validate that the value
 * matches one of the specified accepted values. The validation is performed by {@link
 * io.ascend.flockr.users.annotation.validators.AcceptedValuesConstraintValidator}.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * public void processAction(@AcceptedValues(values = {"append", "remove"}) String action) {
 *   // action will be validated to be either "append" or "remove"
 * }
 * }</pre>
 *
 * <p><strong>Note:</strong> The {@code @Constraint} annotation is currently commented out. To
 * enable automatic validation, uncomment it and ensure the validator is properly configured.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 * @see io.ascend.flockr.users.annotation.validators.AcceptedValuesConstraintValidator
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
// @Constraint(validatedBy = AcceptedValuesConstraintValidator.class)
public @interface AcceptedValues {

  /**
   * Default error message when validation fails.
   *
   * @return the error message
   */
  String message() default "invalid parameter value";

  /**
   * Array of accepted string values.
   *
   * <p>The annotated value must match one of these values (case-sensitive).
   *
   * @return the array of accepted values
   */
  String[] values();

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
