package io.ascend.flockr.admin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that end time is after start time.
 *
 * <p>This annotation should be applied at the class/type level to validate two epoch timestamp
 * fields where one must be after the other.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTimeRangeValidator.class)
@Documented
public @interface ValidTimeRange {
  String message() default "End time must be after start time";

  Class<? extends Payload>[] payload() default {};

  /**
   * Field name for start time.
   *
   * @return start time field name
   */
  String startTimeField() default "startTime";

  /**
   * Field name for end time.
   *
   * @return end time field name
   */
  String endTimeField() default "endTime";
}
