package io.ascend.flockr.admin.validation;

import io.vertx.core.json.JsonObject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link NotNullJsonObject} annotation.
 *
 * <p>Validates that a JsonObject is not null. Empty JsonObjects are considered valid.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public class NotNullJsonObjectValidator
    implements ConstraintValidator<NotNullJsonObject, JsonObject> {

  @Override
  public void initialize(NotNullJsonObject constraintAnnotation) {
    // No initialization needed
  }

  @Override
  public boolean isValid(JsonObject value, ConstraintValidatorContext context) {
    return value != null;
  }
}
