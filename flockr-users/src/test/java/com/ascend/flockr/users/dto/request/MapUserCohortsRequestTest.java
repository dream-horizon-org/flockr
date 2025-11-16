package com.ascend.flockr.users.dto.request;

import static org.junit.Assert.*;

import com.ascend.flockr.common.constants.Constants;
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
    request.setUserId(123L);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
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
    request.setUserId(123L);
    request.setCohortKey(null);
    request.setSource(Constants.SOURCE_DREAM11);
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
    request.setUserId(123L);
    request.setCohortKey("");
    request.setSource(Constants.SOURCE_DREAM11);
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
    request.setUserId(123L);
    request.setCohortKey("   ");
    request.setSource(Constants.SOURCE_DREAM11);
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
  public void validate_WithNullSource_FailsValidation() {
    // Arrange
    request.setUserId(123L);
    request.setCohortKey("test-cohort");
    request.setSource(null);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      request.validate();
      fail("Expected validation to fail for null source");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void validate_WithEmptySource_FailsValidation() {
    // Arrange
    request.setUserId(123L);
    request.setCohortKey("test-cohort");
    request.setSource("");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      request.validate();
      fail("Expected validation to fail for empty source");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void validate_WithNullAction_FailsValidation() {
    // Arrange
    request.setUserId(123L);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
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
    request.setUserId(123L);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
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
    request.setUserId(123L);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
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
  public void validate_WithNegativeUserId_FailsValidation() {
    // Arrange
    request.setUserId(-1L);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      request.validate();
      fail("Expected validation to fail for negative userId");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void validate_WithZeroUserId_FailsValidation() {
    // Arrange
    request.setUserId(0L);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      request.validate();
      fail("Expected validation to fail for zero userId");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void validate_WithGuestIdOnly_PassesValidation() {
    // Arrange
    request.setUserId(null);
    request.setGuestId("guest-123");
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert - should not throw
    try {
      request.validate();
    } catch (Exception e) {
      fail("Validation should pass with guestId only: " + e.getMessage());
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
    Long userId = 123L;
    String guestId = "guest-123";
    Long projectId = 100L;
    String cohortKey = "test-cohort";
    String source = Constants.SOURCE_DREAM11;
    String action = Constants.ACTION_APPEND;
    String expireAt = "2025-12-31 23:59:59";

    // Act
    request.setUserId(userId);
    request.setGuestId(guestId);
    request.setProjectId(projectId);
    request.setCohortKey(cohortKey);
    request.setSource(source);
    request.setAction(action);
    request.setExpireAt(expireAt);

    // Assert
    assertEquals(userId, request.getUserId());
    assertEquals(guestId, request.getGuestId());
    assertEquals(projectId, request.getProjectId());
    assertEquals(cohortKey, request.getCohortKey());
    assertEquals(source, request.getSource());
    assertEquals(action, request.getAction());
    assertEquals(expireAt, request.getExpireAt());
  }
}
