package io.ascend.flockr.admin.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Instant;

/**
 * Validator implementation for {@link ValidFutureEpoch} annotation.
 *
 * <p>Validates that an epoch timestamp is in the future with a minimum delta from the current time.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public class ValidFutureEpochValidator implements ConstraintValidator<ValidFutureEpoch, Long> {

  private long minDeltaSeconds;

  @Override
  public void initialize(ValidFutureEpoch constraintAnnotation) {
    this.minDeltaSeconds = constraintAnnotation.minDeltaSeconds();
  }

  @Override
  public boolean isValid(Long epochTime, ConstraintValidatorContext context) {
    if (epochTime == null) {
      return true;
    }

    long currentTimeSeconds = Instant.now().getEpochSecond();
    long minRequiredTime = currentTimeSeconds + minDeltaSeconds;

    if (epochTime < minRequiredTime) {
      context.disableDefaultConstraintViolation();

      String message;
      if (minDeltaSeconds == 0) {
        message =
            String.format(
                "Time must be in the future. Current time: %d, Provided: %d",
                currentTimeSeconds, epochTime);
      } else {
        message =
            String.format(
                "Time must be at least %d seconds in the future. Current time: %d, Minimum required: %d, Provided: %d",
                minDeltaSeconds, currentTimeSeconds, minRequiredTime, epochTime);
      }

      context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
      return false;
    }

    return true;
  }
}
