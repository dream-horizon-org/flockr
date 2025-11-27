package io.ascend.flockr.admin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a JsonObject is not null and not empty.
 *
 * <p>This annotation can be used on JsonObject fields to ensure they contain at least one key-value
 * pair.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotEmptyJsonObjectValidator.class)
@Documented
public @interface NotEmptyJsonObject {
  String message() default "JsonObject must not be null or empty";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
