package io.ascend.flockr.admin.validation;

import io.ascend.flockr.admin.io.request.CreateRulesRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link ValidTimeRange} annotation.
 *
 * <p>Validates that end time is after start time.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public class ValidTimeRangeValidator
    implements ConstraintValidator<ValidTimeRange, CreateRulesRequest.Rule> {

  private static final Integer DEFAULT_DIFF_SECONDS = 300;
  private String startTimeField;
  private String endTimeField;

  @Override
  public void initialize(ValidTimeRange constraintAnnotation) {
    this.startTimeField = constraintAnnotation.startTimeField();
    this.endTimeField = constraintAnnotation.endTimeField();
  }

  @Override
  public boolean isValid(CreateRulesRequest.Rule rule, ConstraintValidatorContext context) {
    if (rule == null) {
      return true;
    }
    Long startTime = rule.getStartTime();
    Long endTime = rule.getEndTime();
    if (startTime == null || endTime == null) {
      return true;
    }
    if (endTime + DEFAULT_DIFF_SECONDS <= startTime) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              String.format(
                  "End time must be after start time. Start time: %d, End time: %d",
                  startTime, endTime))
          .addPropertyNode(endTimeField)
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
