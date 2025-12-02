package io.ascend.flockr.admin.validation;

import io.vertx.core.json.JsonObject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link NotEmptyJsonObject} annotation.
 *
 * <p>Validates that a JsonObject is not null and contains at least one key-value pair.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public class NotEmptyJsonObjectValidator
    implements ConstraintValidator<NotEmptyJsonObject, JsonObject> {

  @Override
  public void initialize(NotEmptyJsonObject constraintAnnotation) {
    // No initialization needed
  }

  @Override
  public boolean isValid(JsonObject value, ConstraintValidatorContext context) {
    if (value == null) {
      return false;
    }
    return !value.isEmpty();
  }
}
