package com.ascend.flockr.validator;

import com.ascend.flockr.annotation.AcceptedValues;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AcceptedValuesConstraintValidator
    implements ConstraintValidator<AcceptedValues, String> {

  private Set<String> acceptedValues;

  @Override
  public void initialize(AcceptedValues annotation) {
    this.acceptedValues = Arrays.stream(annotation.values()).collect(Collectors.toSet());
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // use @NotNull if value cannot be null
    if (value == null) {
      return true;
    }

    return acceptedValues.contains(value);
  }
}
