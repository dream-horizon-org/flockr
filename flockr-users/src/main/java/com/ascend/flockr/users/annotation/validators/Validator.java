package com.ascend.flockr.users.annotation.validators;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.util.Set;

/**
 * Utility class for programmatic validation of objects using Jakarta Bean Validation.
 *
 * <p>This class provides static methods to validate objects and their properties programmatically.
 * It uses the default validator factory to create a validator instance that checks constraints
 * defined via annotations (e.g., {@link com.ascend.flockr.users.annotation.DateTimeFormat}, {@link
 * com.ascend.flockr.users.annotation.AcceptedValues}).
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * // Validate entire object
 * Validator.validateConstraint(myObject);
 *
 * // Validate specific property
 * Validator.validateConstraint(myObject, "expireAt");
 *
 * // Validate with validation groups
 * Validator.validateConstraint(myObject, MyValidationGroup.class);
 * }</pre>
 *
 * <p><strong>Error Handling:</strong>
 *
 * <p>If validation fails, a {@link ConstraintViolationException} is thrown containing all
 * constraint violations. The exception can be caught and handled appropriately.
 *
 * <p>This class cannot be instantiated. All methods are static.
 *
 * @author Flockr Team
 * @since 1.0
 * @see jakarta.validation.Validator
 * @see ConstraintViolationException
 */
public class Validator {

  /** Private constructor to prevent instantiation. */
  private Validator() {}

  /** Default validator instance for performing validations. */
  private static final jakarta.validation.Validator validateProperty =
      Validation.buildDefaultValidatorFactory().getValidator();

  /**
   * Validates all constraints on the given object.
   *
   * <p>This method validates all properties of the object that have constraint annotations. If any
   * violations are found, a {@link ConstraintViolationException} is thrown.
   *
   * @param <T> the type of object to validate
   * @param object the object to validate
   * @throws ConstraintViolationException if validation fails (contains all violations)
   */
  public static <T> void validateConstraint(T object) {
    Set<ConstraintViolation<T>> violations = validateProperty.validate(object);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  /**
   * Validates constraints on a specific property of the given object.
   *
   * <p>This method validates only the specified property, ignoring other properties. This is useful
   * for partial validation or when you only need to check a single field.
   *
   * @param <T> the type of object to validate
   * @param object the object containing the property to validate
   * @param propertyName the name of the property to validate
   * @throws ConstraintViolationException if validation fails (contains violations for the specified
   *     property)
   */
  public static <T> void validateConstraint(T object, String propertyName) {
    Set<ConstraintViolation<T>> violations =
        validateProperty.validateProperty(object, propertyName);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  /**
   * Validates constraints on the given object for the specified validation groups.
   *
   * <p>This method validates only constraints that belong to the specified validation groups. This
   * allows for conditional validation based on context (e.g., create vs. update scenarios).
   *
   * @param <T> the type of object to validate
   * @param object the object to validate
   * @param group the validation groups to validate (varargs)
   * @throws ConstraintViolationException if validation fails (contains violations for the specified
   *     groups)
   */
  public static <T> void validateConstraint(T object, Class<?>... group) {
    Set<ConstraintViolation<T>> violations = validateProperty.validate(object, group);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
