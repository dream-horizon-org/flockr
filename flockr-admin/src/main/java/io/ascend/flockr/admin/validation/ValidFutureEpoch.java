package io.ascend.flockr.admin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that an epoch timestamp is in the future with an optional minimum delta from current
 * time.
 *
 * <p>This annotation can be applied to any Long field representing an epoch timestamp (Unix
 * timestamp in seconds) to ensure it is in the future.
 *
 * <p>The time value is expected to be in epoch seconds (Unix timestamp).
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidFutureEpochValidator.class)
@Documented
public @interface ValidFutureEpoch {
  String message() default "Time must be in the future";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /**
   * Minimum delta in seconds that the time must be ahead of current time.
   *
   * <p>Default is 0 seconds, meaning the time just needs to be in the future.
   *
   * @return minimum delta in seconds
   */
  long minDeltaSeconds() default 0;
}
