package com.ascend.flockr.common.annotation;

import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
// @Constraint(validatedBy = AcceptedValuesConstraintValidator.class)
public @interface AcceptedValues {

  String message() default "invalid parameter value";

  String[] values();

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
