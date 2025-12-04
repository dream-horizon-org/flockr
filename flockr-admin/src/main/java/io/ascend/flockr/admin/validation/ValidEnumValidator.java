package io.ascend.flockr.admin.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for the {@link ValidEnum} annotation.
 *
 * <p>This validator checks if a String value matches any of the constant names in the specified
 * enum class. The comparison is case-sensitive.
 *
 * @author Flockr Team
 * @since 1.0
 */
public class ValidEnumValidator implements ConstraintValidator<ValidEnum, String> {

  private Set<String> allowedValues;

  @Override
  public void initialize(ValidEnum annotation) {
    allowedValues =
        Arrays.stream(annotation.enumClass().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toSet());
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // Null values are handled by @NotNull annotation
    if (value == null) {
      return true;
    }
    return allowedValues.contains(value);
  }
}
