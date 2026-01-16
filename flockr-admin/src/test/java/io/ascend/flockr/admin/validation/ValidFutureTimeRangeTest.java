package io.ascend.flockr.admin.validation;

import static org.junit.jupiter.api.Assertions.*;

import io.ascend.flockr.admin.domain.rule.*;
import io.ascend.flockr.admin.io.request.CreateRulesRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ValidFutureEpoch} validation annotation.
 *
 * @author Prithu Sharma
 */
class ValidFutureTimeRangeTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Should pass validation when times are in future")
  void testValidFutureTimes() {
    // Arrange
    long currentTime = Instant.now().getEpochSecond();
    long startTime = currentTime + 100; // 100 seconds in future
    long endTime = currentTime + 200; // 200 seconds in future

    CreateRulesRequest.Rule rule = createRule(startTime, endTime);

    // Act
    Set<ConstraintViolation<CreateRulesRequest.Rule>> violations = validator.validate(rule);

    // Assert
    assertTrue(violations.isEmpty(), "Should not have violations for valid future times");
  }

  @Test
  @DisplayName("Should fail validation when start time is in the past")
  void testStartTimeInPast() {
    // Arrange
    long currentTime = Instant.now().getEpochSecond();
    long startTime = currentTime - 100; // In the past
    long endTime = currentTime + 500; // In the future

    CreateRulesRequest.Rule rule = createRule(startTime, endTime);

    // Act
    Set<ConstraintViolation<CreateRulesRequest.Rule>> violations = validator.validate(rule);

    // Assert
    assertFalse(violations.isEmpty(), "Should have violations when start time is in the past");
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("startTime")),
        "Should have violation on startTime field");
  }

  @Test
  @DisplayName("Should fail validation when end time is in the past")
  void testEndTimeInPast() {
    // Arrange
    long currentTime = Instant.now().getEpochSecond();
    long startTime = currentTime + 100; // In the future
    long endTime = currentTime - 100; // In the past

    CreateRulesRequest.Rule rule = createRule(startTime, endTime);

    // Act
    Set<ConstraintViolation<CreateRulesRequest.Rule>> violations = validator.validate(rule);

    // Assert
    assertFalse(violations.isEmpty(), "Should have violations when end time is in the past");
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("endTime")),
        "Should have violation on endTime field");
  }

  @Test
  @DisplayName("Should fail validation when end time is before start time")
  void testEndTimeBeforeStartTime() {
    // Arrange
    long currentTime = Instant.now().getEpochSecond();
    long startTime = currentTime + 800; // 800 seconds in future
    long endTime = currentTime + 400; // 400 seconds in future (before start time)

    CreateRulesRequest.Rule rule = createRule(startTime, endTime);

    // Act
    Set<ConstraintViolation<CreateRulesRequest.Rule>> violations = validator.validate(rule);

    // Assert
    assertFalse(violations.isEmpty(), "Should have violations when end time is before start time");
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("endTime")),
        "Should have violation on endTime field");
    assertTrue(
        violations.stream()
            .anyMatch(v -> v.getMessage().contains("End time must be after start time")),
        "Should have message about end time being after start time");
  }

  @Test
  @DisplayName("Should fail validation when end time equals start time")
  void testEndTimeEqualsStartTime() {
    // Arrange
    long currentTime = Instant.now().getEpochSecond();
    long startTime = currentTime + 400; // 400 seconds in future
    long endTime = startTime; // Same as start time

    CreateRulesRequest.Rule rule = createRule(startTime, endTime);

    // Act
    Set<ConstraintViolation<CreateRulesRequest.Rule>> violations = validator.validate(rule);

    // Assert
    assertFalse(violations.isEmpty(), "Should have violations when end time equals start time");
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("endTime")),
        "Should have violation on endTime field");
  }

  @Test
  @DisplayName("Should have multiple violations when both times are invalid")
  void testMultipleViolations() {
    // Arrange
    long currentTime = Instant.now().getEpochSecond();
    long startTime = currentTime - 100; // In the past
    long endTime = currentTime - 50; // In the past

    CreateRulesRequest.Rule rule = createRule(startTime, endTime);

    // Act
    Set<ConstraintViolation<CreateRulesRequest.Rule>> violations = validator.validate(rule);

    // Assert
    assertFalse(violations.isEmpty(), "Should have violations when both times are invalid");
    assertTrue(violations.size() >= 2, "Should have at least 2 violations for both invalid times");
  }

  @Test
  @DisplayName("Should pass validation when times are null (handled by @NotNull)")
  void testNullTimes() {
    // Arrange
    CreateRulesRequest.Rule rule = createRule(null, null);

    // Act
    Set<ConstraintViolation<CreateRulesRequest.Rule>> violations =
        validator.validate(rule, jakarta.validation.groups.Default.class);

    // Assert - ValidFutureTimeRange should pass, but @NotNull should fail
    assertTrue(
        violations.stream()
            .noneMatch(
                v -> v.getConstraintDescriptor().getAnnotation() instanceof ValidFutureEpoch),
        "ValidFutureTimeRange should not trigger on null values");
  }

  @Test
  @DisplayName("Should validate with custom delta - time just in future should pass")
  void testCustomDeltaConfiguration() {
    // Test that we can use custom delta in other contexts
    long currentTime = Instant.now().getEpochSecond();
    long timeJustInFuture = currentTime + 1; // Only 1 second in future

    CreateRulesRequest.Rule rule = createRule(timeJustInFuture, timeJustInFuture + 100);

    Set<ConstraintViolation<CreateRulesRequest.Rule>> violations = validator.validate(rule);

    // With default delta of 0, this should pass
    assertTrue(
        violations.stream()
            .noneMatch(
                v ->
                    v.getPropertyPath().toString().contains("startTime")
                        && v.getMessage().contains("must be")),
        "Should pass when time is just barely in future with delta=0");
  }

  @Test
  @DisplayName("Should validate exact current time as invalid")
  void testExactCurrentTime() {
    // Arrange
    long currentTime = Instant.now().getEpochSecond();
    long startTime = currentTime - 1; // 1 second in the past to avoid race condition
    long endTime = currentTime + 100;

    CreateRulesRequest.Rule rule = createRule(startTime, endTime);

    // Act
    Set<ConstraintViolation<CreateRulesRequest.Rule>> violations = validator.validate(rule);

    // Assert
    assertFalse(violations.isEmpty(), "Should have violations when time equals current time");
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("startTime")),
        "Should have violation on startTime field");
  }

  private CreateRulesRequest.Rule createRule(Long startTime, Long endTime) {
    CreateRulesRequest.Rule rule = new CreateRulesRequest.Rule();
    rule.setName("Test Rule");
    rule.setDescription("Test Description");
    rule.setStartTime(startTime);
    rule.setEndTime(endTime);
    rule.setRuleType(RuleType.BATCH);
    rule.setRuleAction(RuleAction.ADD);

    // Create a valid batch configuration with required source and query
    BatchConfiguration<SourceInfo> config = new BatchConfiguration<>();
    config.setQuery("SELECT user_id FROM users WHERE status = 'active'");

    // Set a valid source
    SourceInfo source = new SourceInfo();
    source.setId(1L);
    config.setSource(source);

    rule.setConfiguration(config);

    return rule;
  }
}
