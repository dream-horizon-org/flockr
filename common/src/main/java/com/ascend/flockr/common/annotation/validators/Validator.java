package com.ascend.flockr.common.annotation.validators;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.util.Set;

public class Validator {

  private Validator() {}

  private static final jakarta.validation.Validator validateProperty =
      Validation.buildDefaultValidatorFactory().getValidator();

  public static <T> void validateConstraint(T object) {
    Set<ConstraintViolation<T>> violations = validateProperty.validate(object);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  public static <T> void validateConstraint(T object, String propertyName) {
    Set<ConstraintViolation<T>> violations =
        validateProperty.validateProperty(object, propertyName);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  public static <T> void validateConstraint(T object, Class<?>... group) {
    Set<ConstraintViolation<T>> violations = validateProperty.validate(object, group);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
