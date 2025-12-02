package io.ascend.flockr.admin.util;

import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.impl.AsyncResultSingle;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility for performing Jakarta Bean Validation asynchronously on Vert.x worker threads.
 *
 * <p>This utility offloads blocking validation operations (such as complex constraint validators,
 * SQL parsing validators, or any I/O-bound validation) to Vert.x worker threads to avoid blocking
 * the event loop.
 *
 * <p>The validation is executed on a worker thread pool, and the result is delivered back to the
 * event loop thread via RxJava's {@link Single} reactive type.
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * AsyncJakartaValidationUtil.validate(request)
 *     .flatMap(validRequest -> service.processRequest(validRequest))
 *     .subscribe(
 *         result -> log.info("Success: {}", result),
 *         error -> log.error("Validation failed", error)
 *     );
 * }</pre>
 *
 * <p><b>Thread Model:</b>
 *
 * <ul>
 *   <li>Validation is executed on a Vert.x worker thread (not the event loop)
 *   <li>Results are delivered back on the event loop thread
 *   <li>Multiple validations can execute concurrently (unordered execution)
 * </ul>
 *
 * <p><b>Note:</b> This class uses static injection via Guice. Ensure {@code
 * requestStaticInjection(AsyncJakartaValidationUtil.class)} is called in your Guice module.
 *
 * @author Flockr Team
 * @since 1.0
 * @see jakarta.validation.Validator
 * @see io.vertx.core.Vertx#executeBlocking
 */
@Slf4j
@UtilityClass
public class AsyncJakartaValidationUtil {

  @Inject private static Vertx vertx;
  @Inject private static Validator validator;

  /**
   * Validates an object asynchronously on a Vert.x worker thread.
   *
   * <p>This method executes Jakarta Bean Validation on a worker thread to prevent blocking the
   * event loop. The validation uses all constraint annotations defined on the object's class.
   *
   * <p>The returned {@link Single} will:
   *
   * <ul>
   *   <li>Emit the validated object on success
   *   <li>Emit a {@link ValidationException} if validation constraints are violated
   *   <li>Emit a {@link RuntimeException} for unexpected validation errors
   * </ul>
   *
   * @param object the object to validate; must not be {@code null}
   * @param <T> the type of object being validated
   * @return a {@link Single} emitting the validated object if valid, or an error if validation
   *     fails
   * @throws NullPointerException if {@code object} is {@code null}
   * @throws IllegalStateException if Guice dependencies have not been initialized
   */
  public static <T> Single<T> validate(T object) {
    ensureInitialized();
    if (object == null) {
      return Single.error(new NullPointerException("Object to validate must not be null"));
    }

    return AsyncResultSingle.toSingle(
        asyncResultHandler ->
            vertx
                .getDelegate()
                .executeBlocking(
                    () -> {
                      try {
                        log.debug(
                            "Performing validation on worker thread for: {}",
                            object.getClass().getSimpleName());
                        Set<ConstraintViolation<T>> violations = validator.validate(object);

                        if (violations.isEmpty()) {
                          log.debug("Validation successful");
                          return object;
                        } else {
                          String errorMessage = buildErrorMessage(violations);
                          log.warn("Validation failed: {}", errorMessage);
                          throw new ValidationException(errorMessage, violations);
                        }
                      } catch (ValidationException e) {
                        throw e; // Re-throw validation exceptions as-is
                      } catch (Exception e) {
                        log.error("Validation error", e);
                        throw new RuntimeException("Validation failed: " + e.getMessage(), e);
                      }
                    },
                    false)
                .onComplete(asyncResultHandler));
  }

  /**
   * Validates a specific property of an object asynchronously on a Vert.x worker thread.
   *
   * <p>This method executes Jakarta Bean Validation for a single property on a worker thread. Only
   * the constraints defined on the specified property are validated.
   *
   * <p>The returned {@link Single} will:
   *
   * <ul>
   *   <li>Emit the original object on success (property is valid)
   *   <li>Emit a {@link ValidationException} if the property violates constraints
   *   <li>Emit a {@link RuntimeException} for unexpected validation errors
   * </ul>
   *
   * @param object the object containing the property to validate; must not be {@code null}
   * @param propertyName the name of the property to validate; must not be {@code null} or blank
   * @param <T> the type of object containing the property
   * @return a {@link Single} emitting the object if the property is valid, or an error if
   *     validation fails
   * @throws NullPointerException if {@code object} is {@code null}
   * @throws IllegalArgumentException if {@code propertyName} is {@code null} or blank
   * @throws IllegalStateException if Guice dependencies have not been initialized
   */
  public static <T> Single<T> validateProperty(T object, String propertyName) {
    ensureInitialized();
    if (object == null) {
      return Single.error(new NullPointerException("Object to validate must not be null"));
    }
    if (propertyName == null || propertyName.isBlank()) {
      return Single.error(new IllegalArgumentException("Property name must not be null or blank"));
    }

    return AsyncResultSingle.toSingle(
        asyncResultHandler ->
            vertx
                .getDelegate()
                .executeBlocking(
                    () -> {
                      try {
                        log.debug(
                            "Validating property '{}' on worker thread for: {}",
                            propertyName,
                            object.getClass().getSimpleName());

                        Set<ConstraintViolation<T>> violations =
                            validator.validateProperty(object, propertyName);

                        if (violations.isEmpty()) {
                          log.debug("Property validation successful");
                          return object;
                        } else {
                          String errorMessage = buildErrorMessage(violations);
                          log.warn("Property validation failed: {}", errorMessage);
                          throw new ValidationException(errorMessage, violations);
                        }
                      } catch (ValidationException e) {
                        throw e;
                      } catch (Exception e) {
                        log.error("Property validation error", e);
                        throw new RuntimeException(
                            "Property validation failed: " + e.getMessage(), e);
                      }
                    },
                    false)
                .onComplete(asyncResultHandler));
  }

  /**
   * Ensures that static dependencies (Vertx and Validator) have been injected by Guice.
   *
   * <p>This method should be called at the start of every public method to fail fast if the utility
   * has not been properly initialized.
   *
   * @throws IllegalStateException if {@code vertx} or {@code validator} is {@code null}
   */
  private static void ensureInitialized() {
    if (vertx == null || validator == null) {
      throw new IllegalStateException(
          "AsyncJakartaValidationUtil not initialized. "
              + "Ensure requestStaticInjection(AsyncJakartaValidationUtil.class) "
              + "is called in your Guice module.");
    }
  }

  /**
   * Builds a user-friendly error message from validation violations.
   *
   * <p>The message format is: {@code "propertyPath1: message1; propertyPath2: message2; ..."}
   *
   * @param violations the set of constraint violations; must not be empty
   * @param <T> the type of object that was validated
   * @return a semicolon-separated string of all violation messages with their property paths
   */
  private static <T> String buildErrorMessage(Set<ConstraintViolation<T>> violations) {
    return violations.stream()
        .map(v -> String.format("%s: %s", v.getPropertyPath().toString(), v.getMessage()))
        .collect(Collectors.joining("; "));
  }

  /**
   * Exception thrown when Jakarta Bean Validation constraints are violated.
   *
   * <p>This exception wraps the set of {@link ConstraintViolation} instances that describe which
   * constraints failed and why. The exception message contains a human-readable summary of all
   * violations.
   *
   * <p><b>Usage example:</b>
   *
   * <pre>{@code
   * AsyncJakartaValidationUtil.validate(request)
   *     .subscribe(
   *         result -> process(result),
   *         error -> {
   *             if (error instanceof ValidationException ve) {
   *                 ve.getViolations().forEach(v ->
   *                     log.warn("{}: {}", v.getPropertyPath(), v.getMessage())
   *                 );
   *             }
   *         }
   *     );
   * }</pre>
   *
   * @see ConstraintViolation
   */
  @Getter
  public static class ValidationException extends RuntimeException {

    /** The set of constraint violations that caused this exception. */
    private final Set<? extends ConstraintViolation<?>> violations;

    /**
     * Constructs a new ValidationException with the specified message and violations.
     *
     * @param message a human-readable summary of the validation failures
     * @param violations the set of constraint violations; stored as-is (not copied)
     * @param <T> the type of object that was validated
     */
    public <T> ValidationException(String message, Set<ConstraintViolation<T>> violations) {
      super(message);
      this.violations = violations;
    }
  }
}
