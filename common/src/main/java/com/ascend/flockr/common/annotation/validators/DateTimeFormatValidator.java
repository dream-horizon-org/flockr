package com.ascend.flockr.common.annotation.validators;

import com.ascend.flockr.common.annotation.DateTimeFormat;


import com.ascend.flockr.common.utils.CommonUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateTimeFormatValidator implements ConstraintValidator<DateTimeFormat, String> {

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
