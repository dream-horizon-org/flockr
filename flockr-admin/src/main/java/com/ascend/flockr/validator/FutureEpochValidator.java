package com.ascend.flockr.validator;

import com.ascend.flockr.annotation.FutureEpoch;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FutureEpochValidator implements ConstraintValidator<FutureEpoch, Long> {
  private FutureEpoch.TimeUnit unit;

  @Override
  public void initialize(FutureEpoch constraints) {
    unit = constraints.unit();
  }

  @Override
  public boolean isValid(Long value, ConstraintValidatorContext context) {

    if (value == null) {
      return true;
    }
    if (unit == FutureEpoch.TimeUnit.SECONDS) {
      value *= 1000;
    }
    return (value > System.currentTimeMillis());
  }
}
