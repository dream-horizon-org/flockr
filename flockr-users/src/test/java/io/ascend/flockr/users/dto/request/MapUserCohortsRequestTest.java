package io.ascend.flockr.users.dto.request;

import static org.junit.Assert.*;

import io.ascend.flockr.users.constants.Constants;
import jakarta.validation.ConstraintViolationException;
import org.junit.Test;

/**
 * Unit tests for {@link MapUserCohortsRequest} business logic.
 *
 * <p>Tests cover date parsing, expiry epoch calculation, and validation logic.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
public class MapUserCohortsRequestTest {

  @Test
  public void expiryEpochFromExpireAt_WithValidFutureDate_ReturnsEpochMillis() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
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
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setExpireAt("2020-01-01 00:00:00");
    request.setAction(Constants.ACTION_APPEND);

    // Act & Assert
    try {
      request.expiryEpochFromExpireAt();
      fail("Expected exception to be thrown for past date in append action");
    } catch (Exception e) {
      // RuntimeException or IllegalArgumentException is thrown for invalid expiry time
      assertNotNull(e);
    }
  }

  @Test
  public void expiryEpochFromExpireAt_WithPastDateForRemove_ReturnsZero() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
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
    MapUserCohortsRequest request = new MapUserCohortsRequest();
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
    MapUserCohortsRequest request = new MapUserCohortsRequest();
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
    MapUserCohortsRequest request = new MapUserCohortsRequest();
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

  @Test(expected = ConstraintViolationException.class)
  public void validate_WithNullCohortKey_FailsValidation() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey(null);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act
    request.validate();
  }

  @Test(expected = ConstraintViolationException.class)
  public void validate_WithEmptyCohortKey_FailsValidation() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act
    request.validate();
  }

  @Test(expected = ConstraintViolationException.class)
  public void validate_WithBlankCohortKey_FailsValidation() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("   ");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act
    request.validate();
  }

  @Test(expected = ConstraintViolationException.class)
  public void validate_WithNullAction_FailsValidation() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(null);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act
    request.validate();
  }

  @Test(expected = ConstraintViolationException.class)
  public void validate_WithEmptyAction_FailsValidation() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction("");
    request.setExpireAt("2025-12-31 23:59:59");

    // Act
    request.validate();
  }

  @Test(expected = ConstraintViolationException.class)
  public void validate_WithNullExpireAt_FailsValidation() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt(null);

    // Act
    request.validate();
  }

  @Test(expected = ConstraintViolationException.class)
  public void validate_WithInvalidAction_FailsValidation() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction("invalid-action");
    request.setExpireAt("2025-12-31 23:59:59");

    // Act
    request.validate();
  }
}
