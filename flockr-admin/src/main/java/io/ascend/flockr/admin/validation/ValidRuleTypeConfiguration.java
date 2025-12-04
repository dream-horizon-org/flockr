package io.ascend.flockr.admin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that the ruleType matches the configuration type.
 *
 * <p>This annotation ensures that when ruleType is STREAM, the configuration type is also STREAM,
 * and when ruleType is BATCH, the configuration type is also BATCH.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidRuleTypeConfigurationValidator.class)
@Documented
public @interface ValidRuleTypeConfiguration {
  String message() default "Rule type must match configuration type";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
