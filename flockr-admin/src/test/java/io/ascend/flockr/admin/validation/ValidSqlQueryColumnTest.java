package io.ascend.flockr.admin.validation;

import static org.junit.jupiter.api.Assertions.*;

import io.ascend.flockr.admin.domain.rule.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ValidSqlQuery} validation annotation with required column validation.
 *
 * @author Prithu Sharma
 */
class ValidSqlQueryColumnTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Should pass validation when query contains only required user_id column")
  void testValidQueryWithUserId() {
    // Arrange
    BatchConfiguration<SourceInfo> config = new BatchConfiguration<>();
    config.setQuery("SELECT user_id FROM users WHERE status = 'active'");
    config.setSource(createSource());

    // Act
    Set<ConstraintViolation<BatchConfiguration<SourceInfo>>> violations =
        validator.validate(config);

    // Assert
    assertTrue(violations.isEmpty(), "Should not have violations when only user_id is selected");
  }

  @Test
  @DisplayName("Should fail validation when query uses SELECT * (selects all columns)")
  void testInvalidQueryWithSelectAll() {
    // Arrange
    BatchConfiguration<SourceInfo> config = new BatchConfiguration<>();
    config.setQuery("SELECT * FROM users WHERE status = 'active'");
    config.setSource(createSource());

    // Act
    Set<ConstraintViolation<BatchConfiguration<SourceInfo>>> violations =
        validator.validate(config);

    // Assert
    assertFalse(violations.isEmpty(), "Should have violations when SELECT * is used");
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("ONLY the required column")),
        "Should have error about SELECT * selecting all columns");
  }

  @Test
  @DisplayName("Should pass validation when query has only user_id with table prefix")
  void testValidQueryWithTablePrefix() {
    // Arrange
    BatchConfiguration<SourceInfo> config = new BatchConfiguration<>();
    config.setQuery("SELECT u.user_id FROM users u WHERE u.status = 'active'");
    config.setSource(createSource());

    // Act
    Set<ConstraintViolation<BatchConfiguration<SourceInfo>>> violations =
        validator.validate(config);

    // Assert
    assertTrue(
        violations.isEmpty(),
        "Should not have violations when only user_id is selected with table prefix");
  }

  @Test
  @DisplayName("Should pass validation when query has only user_id with alias")
  void testValidQueryWithAlias() {
    // Arrange
    BatchConfiguration<SourceInfo> config = new BatchConfiguration<>();
    config.setQuery("SELECT user_id AS userId FROM users WHERE status = 'active'");
    config.setSource(createSource());

    // Act
    Set<ConstraintViolation<BatchConfiguration<SourceInfo>>> violations =
        validator.validate(config);

    // Assert
    assertTrue(
        violations.isEmpty(),
        "Should not have violations when only user_id is selected with alias");
  }

  @Test
  @DisplayName("Should pass validation with case-insensitive column name matching")
  void testValidQueryCaseInsensitive() {
    // Arrange
    BatchConfiguration<SourceInfo> config = new BatchConfiguration<>();
    config.setQuery("SELECT USER_ID FROM users WHERE status = 'active'");
    config.setSource(createSource());

    // Act
    Set<ConstraintViolation<BatchConfiguration<SourceInfo>>> violations =
        validator.validate(config);

    // Assert
    assertTrue(
        violations.isEmpty(),
        "Should not have violations with uppercase USER_ID (case-insensitive)");
  }

  @Test
  @DisplayName("Should fail validation when query selects multiple columns")
  void testInvalidQueryMultipleColumns() {
    // Arrange
    BatchConfiguration<SourceInfo> config = new BatchConfiguration<>();
    config.setQuery("SELECT user_id, name, email FROM users WHERE status = 'active'");
    config.setSource(createSource());

    // Act
    Set<ConstraintViolation<BatchConfiguration<SourceInfo>>> violations =
        validator.validate(config);

    // Assert
    assertFalse(violations.isEmpty(), "Should have violations when multiple columns are selected");
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("ONLY the required column")),
        "Should have error message about selecting only user_id");
  }

  @Test
  @DisplayName("Should fail validation when query is missing required user_id column")
  void testInvalidQueryMissingUserId() {
    // Arrange
    BatchConfiguration<SourceInfo> config = new BatchConfiguration<>();
    config.setQuery("SELECT name FROM users WHERE status = 'active'");
    config.setSource(createSource());

    // Act
    Set<ConstraintViolation<BatchConfiguration<SourceInfo>>> violations =
        validator.validate(config);

    // Assert
    assertFalse(violations.isEmpty(), "Should have violations when user_id is missing");
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("ONLY the required column")),
        "Should have error message about required column");
  }

  @Test
  @DisplayName("Should fail validation when query only has user_name (not user_id)")
  void testInvalidQueryWrongColumn() {
    // Arrange
    BatchConfiguration<SourceInfo> config = new BatchConfiguration<>();
    config.setQuery("SELECT user_name, user_email FROM users WHERE status = 'active'");
    config.setSource(createSource());

    // Act
    Set<ConstraintViolation<BatchConfiguration<SourceInfo>>> violations =
        validator.validate(config);

    // Assert
    assertFalse(violations.isEmpty(), "Should have violations when user_id is not present");
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("query")),
        "Should have violation on query field");
  }

  @Test
  @DisplayName("Should pass validation for subquery with only user_id in outer SELECT")
  void testValidSubquery() {
    // Arrange
    BatchConfiguration<SourceInfo> config = new BatchConfiguration<>();
    config.setQuery(
        "SELECT user_id FROM (SELECT user_id, name FROM users WHERE status = 'active') AS active_users");
    config.setSource(createSource());

    // Act
    Set<ConstraintViolation<BatchConfiguration<SourceInfo>>> violations =
        validator.validate(config);

    // Assert
    assertTrue(
        violations.isEmpty(),
        "Should not have violations when outermost SELECT contains only user_id");
  }

  @Test
  @DisplayName("Should still validate SQL syntax along with column requirement")
  void testInvalidSqlSyntax() {
    // Arrange
    BatchConfiguration<SourceInfo> config = new BatchConfiguration<>();
    config.setQuery("SELECT user_id FROM users WHERE"); // Invalid SQL - incomplete WHERE clause
    config.setSource(createSource());

    // Act
    Set<ConstraintViolation<BatchConfiguration<SourceInfo>>> violations =
        validator.validate(config);

    // Assert
    assertFalse(violations.isEmpty(), "Should have violations for invalid SQL syntax");
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("Invalid SQL syntax")),
        "Should have SQL syntax error message");
  }

  private SourceInfo createSource() {
    SourceInfo source = new SourceInfo();
    source.setId(1L);
    return source;
  }
}
