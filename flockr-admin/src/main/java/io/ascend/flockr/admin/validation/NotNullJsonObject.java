package io.ascend.flockr.admin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a JsonObject is not null.
 *
 * <p>This annotation can be used on JsonObject fields to ensure they are not null. Note that this
 * allows empty JsonObjects. Use {@link NotEmptyJsonObject} if you also want to ensure the
 * JsonObject is not empty.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotNullJsonObjectValidator.class)
@Documented
public @interface NotNullJsonObject {
  String message() default "JsonObject must not be null";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
