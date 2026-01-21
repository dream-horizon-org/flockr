package io.ascend.flockr.admin.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

/**
 * Validator implementation for {@link ValidSqlQuery} annotation.
 *
 * <p>Validates SQL query syntax using JSqlParser to ensure queries are syntactically correct.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
public class ValidSqlQueryValidator implements ConstraintValidator<ValidSqlQuery, String> {

  @Override
  public void initialize(ValidSqlQuery constraintAnnotation) {
    // No initialization needed
  }

  @Override
  public boolean isValid(String query, ConstraintValidatorContext context) {
    try {
      // Parse the SQL query using JSqlParser
      // If parsing succeeds without throwing an exception, the query is syntactically valid
      CCJSqlParserUtil.parse(query, ccjSqlParser -> ccjSqlParser.withAllowComplexParsing(true));

      log.debug("Successfully validated SQL query syntax");
      return true;

    } catch (Exception e) {
      // Parsing failed - query has syntax errors
      log.error("SQL query syntax validation failed: {}", e.getMessage());

      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate("Invalid SQL syntax: " + extractErrorMessage(e))
          .addConstraintViolation();

      return false;
    }
  }

  /**
   * Extracts a user-friendly error message from the parsing exception.
   *
   * @param e the exception thrown during parsing
   * @return a cleaned error message
   */
  private String extractErrorMessage(Exception e) {
    String message = e.getMessage();
    if (message == null || message.isEmpty()) {
      return "Unable to parse SQL query";
    }

    // Clean up the error message for better readability
    // JSqlParser error messages can be quite verbose
    if (message.length() > 200) {
      message = message.substring(0, 200) + "...";
    }

    return message;
  }
}
