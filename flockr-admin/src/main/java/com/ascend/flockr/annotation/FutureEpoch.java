package com.ascend.flockr.annotation;

import com.ascend.flockr.validator.FutureEpochValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Constraint(validatedBy = FutureEpochValidator.class)
public @interface FutureEpoch {
  String message() default "must be a future date";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  TimeUnit unit();

  enum TimeUnit {
    MILLISECONDS,

    SECONDS
  }
}
