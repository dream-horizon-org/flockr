package com.ascend.flockr.annotation;

import com.ascend.flockr.validator.EnumerationValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Constraint(validatedBy = EnumerationValidator.class)
public @interface Enumeration {

  String message() default "invalid enum value";

  Class<?> enumClass();

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
