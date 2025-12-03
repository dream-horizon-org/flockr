package com.ascend.flockr.users.annotation.validators;

import com.ascend.flockr.users.annotation.AcceptedValues;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator implementation for {@link AcceptedValues} annotation.
 *
 * <p>This validator checks that a string value is one of the accepted values specified in the
 * annotation. The accepted values are stored in a set for O(1) lookup performance.
 *
 * <p><strong>Validation Rules:</strong>
 *
 * <ul>
 *   <li>Null values are considered valid (validation passes)
 *   <li>Non-null values must exactly match one of the accepted values (case-sensitive)
 *   <li>Comparison is done using {@link Set#contains(Object)}
 * </ul>
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * @AcceptedValues(values = {"append", "remove"})
 * String action;
 *
 * // Valid values
 * "append"  // passes
 * "remove"  // passes
 * null      // passes
 *
 * // Invalid values
 * "APPEND"  // fails (case-sensitive)
 * "delete"  // fails (not in accepted list)
 * }</pre>
 *
 * @author Flockr Team
 * @since 1.0
 * @see AcceptedValues
 */
public class AcceptedValuesConstraintValidator
    implements ConstraintValidator<AcceptedValues, String> {
  /** Set of accepted values for fast lookup. */
  private Set<String> acceptedValues;

  /**
   * Initializes the validator with the accepted values from the annotation.
   *
   * <p>This method is called once when the validator is created. It converts the array of accepted
   * values from the annotation into a set for efficient lookup.
   *
   * @param annotation the AcceptedValues annotation instance
   */
  @Override
  public void initialize(AcceptedValues annotation) {
    this.acceptedValues = Arrays.stream(annotation.values()).collect(Collectors.toSet());
  }

  /**
   * Validates that the string value is one of the accepted values.
   *
   * <p>This method checks if the value (case-sensitive) exists in the set of accepted values. Null
   * values are considered valid.
   *
   * @param value the string value to validate, may be null
   * @param context the constraint validator context (unused)
   * @return {@code true} if the value is null or is contained in the accepted values set, {@code
   *     false} otherwise
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return acceptedValues.contains(value);
  }
}
