package io.ascend.flockr.admin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates SQL query syntax using JSqlParser.
 *
 * <p>This annotation ensures that the SQL query string is syntactically valid and can be parsed
 * successfully. Optionally validates that the outermost SELECT statement contains ONLY a specific
 * required column.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSqlQueryValidator.class)
@Documented
public @interface ValidSqlQuery {
  String message() default "SQL query syntax is invalid";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /**
   * Optional column name that must be the ONLY column in the outermost SELECT statement.
   *
   * <p>If specified, the validator will check that the outermost SELECT contains ONLY this column
   * (case-insensitive). Multiple columns or SELECT * are not allowed.
   *
   * @return required column name, or empty string for no column validation
   */
  String requiredColumn() default "";
}
