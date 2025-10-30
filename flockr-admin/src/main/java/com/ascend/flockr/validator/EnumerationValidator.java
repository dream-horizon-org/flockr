package com.ascend.flockr.validator;

import com.ascend.flockr.annotation.Enumeration;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumerationValidator implements ConstraintValidator<Enumeration, String> {

  private Set<String> acceptedValues;

  @Override
  public void initialize(Enumeration annotation) {
    this.acceptedValues =
        Arrays.stream(annotation.enumClass().getEnumConstants())
            .map(Object::toString)
            .collect(Collectors.toSet());
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
