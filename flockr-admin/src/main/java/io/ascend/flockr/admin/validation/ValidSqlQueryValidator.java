package io.ascend.flockr.admin.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;

/**
 * Validator implementation for {@link ValidSqlQuery} annotation.
 *
 * <p>Validates SQL query syntax using JSqlParser to ensure queries are syntactically correct.
 * Optionally validates that the outermost SELECT statement contains ONLY a specific required
 * column.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
public class ValidSqlQueryValidator implements ConstraintValidator<ValidSqlQuery, String> {

  private String requiredColumn;

  @Override
  public void initialize(ValidSqlQuery constraintAnnotation) {
    this.requiredColumn = constraintAnnotation.requiredColumn();
  }

  @Override
  public boolean isValid(String query, ConstraintValidatorContext context) {
    try {
      // Parse the SQL query using JSqlParser
      // If parsing succeeds without throwing an exception, the query is syntactically valid
      Statement statement =
          CCJSqlParserUtil.parse(query, ccjSqlParser -> ccjSqlParser.withAllowComplexParsing(true));

      log.debug("Successfully validated SQL query syntax");

      // If requiredColumn is specified, validate that it exists in the outermost SELECT
      if (requiredColumn != null && !requiredColumn.isEmpty()) {
        if (!validateRequiredColumn(statement, context)) {
          return false;
        }
      }

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
   * Validates that ONLY the required column exists in the outermost SELECT statement.
   *
   * @param statement the parsed SQL statement
   * @param context the validation context
   * @return true if only the required column is selected, false otherwise
   */
  private boolean validateRequiredColumn(Statement statement, ConstraintValidatorContext context) {
    if (!(statement instanceof Select select)) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "Query must be a SELECT statement to validate required column")
          .addConstraintViolation();
      return false;
    }

    // Get the outermost select body
    if (!(select.getSelectBody() instanceof PlainSelect plainSelect)) {
      log.warn(
          "Cannot validate required column for complex SELECT (UNION, etc.). Skipping column validation.");
      return true; // Skip validation for complex queries
    }

    List<SelectItem<?>> selectItems = plainSelect.getSelectItems();

    // Validate that there is exactly one column selected
    if (selectItems.size() != 1) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              String.format(
                  "Query must select ONLY the required column '%s' (found %d columns in SELECT)",
                  requiredColumn, selectItems.size()))
          .addConstraintViolation();
      log.error(
          "Query must select only '{}' column, but found {} columns",
          requiredColumn,
          selectItems.size());
      return false;
    }

    // Check that the single column matches the required column (case-insensitive)
    String itemStr = selectItems.get(0).toString();

    // Reject SELECT * as it selects all columns, not just the required one
    if ("*".equals(itemStr.trim())) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              String.format(
                  "Query must select ONLY the required column '%s', not SELECT *", requiredColumn))
          .addConstraintViolation();
      log.error("Query uses SELECT * instead of selecting only '{}'", requiredColumn);
      return false;
    }

    // Extract column name from "table.column" or "column AS alias"
    String columnName = extractColumnName(itemStr);
    if (!columnName.equalsIgnoreCase(requiredColumn)) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              String.format(
                  "Query must select ONLY the required column '%s', but found '%s'",
                  requiredColumn, columnName))
          .addConstraintViolation();
      log.error("Query must select only '{}' column, but found '{}'", requiredColumn, columnName);
      return false;
    }

    log.debug("Query correctly selects only the required column '{}'", requiredColumn);
    return true;
  }

  /**
   * Extracts the column name from a select item string.
   *
   * <p>Handles formats like: - "column" - "table.column" - "column AS alias" - "table.column AS
   * alias"
   *
   * @param selectItemStr the select item string
   * @return the extracted column name
   */
  private String extractColumnName(String selectItemStr) {
    String trimmed = selectItemStr.trim();

    // Handle "column AS alias" - take the part before AS
    int asIndex = trimmed.toUpperCase().indexOf(" AS ");
    if (asIndex > 0) {
      trimmed = trimmed.substring(0, asIndex).trim();
    }

    // Handle "table.column" - take the part after the last dot
    int dotIndex = trimmed.lastIndexOf('.');
    if (dotIndex > 0 && dotIndex < trimmed.length() - 1) {
      trimmed = trimmed.substring(dotIndex + 1).trim();
    }

    // Remove quotes if present
    trimmed = trimmed.replace("\"", "").replace("`", "").replace("'", "");

    return trimmed;
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
