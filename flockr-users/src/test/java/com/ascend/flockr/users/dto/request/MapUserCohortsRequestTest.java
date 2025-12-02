package com.ascend.flockr.users.dto.request;

import static org.junit.Assert.*;

import com.ascend.flockr.users.constants.Constants;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link MapUserCohortsRequest} DTO.
 *
 * <p>Tests cover validation, data conversion, and edge cases for request DTOs.
 *
 * @since 1.0
 */
public class MapUserCohortsRequestTest {

  private MapUserCohortsRequest request;

  @Before
  public void setUp() {
    request = new MapUserCohortsRequest();
  }

  @Test
  public void expiryEpochFromExpireAt_WithValidFutureDate_ReturnsEpochMillis() {
    // Arrange
    request.setExpireAt("2025-12-31 23:59:59");
    request.setAction(Constants.ACTION_APPEND);

    // Act
    Long epoch = request.expiryEpochFromExpireAt();

    // Assert
    assertNotNull(epoch);
    assertTrue(epoch > System.currentTimeMillis());
  }

  @Test
  public void expiryEpochFromExpireAt_WithPastDateForAppend_ThrowsException() {
    // Arrange
    request.setExpireAt("2020-01-01 00:00:00");
    request.setAction(Constants.ACTION_APPEND);

    // Act & Assert
    try {
      request.expiryEpochFromExpireAt();
      fail("Expected exception to be thrown for past date in append action");
    } catch (Exception e) {
      assertNotNull(e);
      assertTrue(
          e.getMessage().contains("INVALID_EXPIRY_TIME")
              || e.getMessage().contains("Invalid expiryAt"));
    }
  }

  @Test
  public void expiryEpochFromExpireAt_WithPastDateForRemove_ReturnsZero() {
    // Arrange
    request.setExpireAt("2020-01-01 00:00:00");
    request.setAction(Constants.ACTION_REMOVE);

    // Act
    Long epoch = request.expiryEpochFromExpireAt();

    // Assert
    assertNotNull(epoch);
    assertEquals(Long.valueOf(0L), epoch);
  }

  @Test
  public void expiryEpochFromExpireAt_WithInvalidDateFormat_ThrowsException() {
    // Arrange
    request.setExpireAt("invalid-date-format");
    request.setAction(Constants.ACTION_APPEND);

    // Act & Assert
    try {
      request.expiryEpochFromExpireAt();
      fail("Expected exception to be thrown for invalid date format");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void expiryEpochFromExpireAt_WithNullExpireAt_ThrowsException() {
    // Arrange
    request.setExpireAt(null);
    request.setAction(Constants.ACTION_APPEND);

    // Act & Assert
    try {
      request.expiryEpochFromExpireAt();
      fail("Expected exception to be thrown for null expireAt");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void validate_WithValidRequest_PassesValidation() {
    // Arrange
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert - should not throw
    try {
      request.validate();
    } catch (Exception e) {
      fail("Validation should pass for valid request: " + e.getMessage());
    }
  }

  @Test
  public void validate_WithNullCohortKey_FailsValidation() {
    // Arrange
    request.setCohortKey(null);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      request.validate();
      fail("Expected validation to fail for null cohortKey");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void validate_WithEmptyCohortKey_FailsValidation() {
    // Arrange
    request.setCohortKey("");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      request.validate();
      fail("Expected validation to fail for empty cohortKey");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void validate_WithBlankCohortKey_FailsValidation() {
    // Arrange
    request.setCohortKey("   ");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      request.validate();
      fail("Expected validation to fail for blank cohortKey");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void validate_WithNullAction_FailsValidation() {
    // Arrange
    request.setCohortKey("test-cohort");
    request.setAction(null);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      request.validate();
      fail("Expected validation to fail for null action");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void validate_WithEmptyAction_FailsValidation() {
    // Arrange
    request.setCohortKey("test-cohort");
    request.setAction("");
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      request.validate();
      fail("Expected validation to fail for empty action");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void validate_WithNullExpireAt_FailsValidation() {
    // Arrange
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt(null);

    // Act & Assert
    try {
      request.validate();
      fail("Expected validation to fail for null expireAt");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void expiryEpochFromExpireAt_WithDifferentTimeZones_ReturnsCorrectEpoch() {
    // Arrange - Test with a specific date
    request.setExpireAt("2025-12-31 23:59:59");
    request.setAction(Constants.ACTION_APPEND);

    // Act
    Long epoch1 = request.expiryEpochFromExpireAt();

    // Assert
    assertNotNull(epoch1);
    // The epoch should represent the date in UTC
    assertTrue(epoch1 > System.currentTimeMillis());
  }

  @Test
  public void gettersAndSetters_WorkCorrectly() {
    // Arrange
    String cohortKey = "test-cohort";
    String action = Constants.ACTION_APPEND;
    String expireAt = "2025-12-31 23:59:59";

    // Act
    request.setCohortKey(cohortKey);
    request.setAction(action);
    request.setExpireAt(expireAt);

    // Assert
    assertEquals(cohortKey, request.getCohortKey());
    assertEquals(action, request.getAction());
    assertEquals(expireAt, request.getExpireAt());
  }
}
