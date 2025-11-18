package com.ascend.flockr.common.annotation;


import com.ascend.flockr.common.annotation.validators.DateTimeFormatValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Constraint(validatedBy = DateTimeFormatValidator.class)
public @interface DateTimeFormat {
  String message() default "invalid datetime format value";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
