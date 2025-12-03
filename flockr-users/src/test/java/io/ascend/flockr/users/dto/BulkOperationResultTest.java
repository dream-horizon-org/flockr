package io.ascend.flockr.users.dto;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link BulkOperationResult} DTO.
 *
 * <p>Tests cover constructor, getters, and data integrity for bulk operation results.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
public class BulkOperationResultTest {

  private BulkOperationResult result;

  @Before
  public void setUp() {
    result = new BulkOperationResult();
  }

  @Test
  public void constructor_WithAllParameters_CreatesInstance() {
    // Arrange & Act
    BulkOperationResult result =
        new BulkOperationResult(10, 8, 2, "Processed 10 users. Success: 8, Failed: 2");

    // Assert
    assertNotNull(result);
    assertEquals(10, result.getTotalProcessed());
    assertEquals(8, result.getSuccessCount());
    assertEquals(2, result.getFailedCount());
    assertEquals("Processed 10 users. Success: 8, Failed: 2", result.getMessage());
  }

  @Test
  public void constructor_WithZeroValues_CreatesInstance() {
    // Arrange & Act
    BulkOperationResult result = new BulkOperationResult(0, 0, 0, "No users processed");

    // Assert
    assertNotNull(result);
    assertEquals(0, result.getTotalProcessed());
    assertEquals(0, result.getSuccessCount());
    assertEquals(0, result.getFailedCount());
    assertEquals("No users processed", result.getMessage());
  }

  @Test
  public void constructor_WithAllFailures_CreatesInstance() {
    // Arrange & Act
    BulkOperationResult result =
        new BulkOperationResult(5, 0, 5, "Processed 5 users. Success: 0, Failed: 5");

    // Assert
    assertNotNull(result);
    assertEquals(5, result.getTotalProcessed());
    assertEquals(0, result.getSuccessCount());
    assertEquals(5, result.getFailedCount());
  }

  @Test
  public void constructor_WithAllSuccesses_CreatesInstance() {
    // Arrange & Act
    BulkOperationResult result =
        new BulkOperationResult(10, 10, 0, "Processed 10 users. Success: 10, Failed: 0");

    // Assert
    assertNotNull(result);
    assertEquals(10, result.getTotalProcessed());
    assertEquals(10, result.getSuccessCount());
    assertEquals(0, result.getFailedCount());
  }

  @Test
  public void settersAndGetters_WorkCorrectly() {
    // Arrange
    int totalProcessed = 100;
    int successCount = 95;
    int failedCount = 5;
    String message = "Test message";

    // Act
    result.setTotalProcessed(totalProcessed);
    result.setSuccessCount(successCount);
    result.setFailedCount(failedCount);
    result.setMessage(message);

    // Assert
    assertEquals(totalProcessed, result.getTotalProcessed());
    assertEquals(successCount, result.getSuccessCount());
    assertEquals(failedCount, result.getFailedCount());
    assertEquals(message, result.getMessage());
  }

  @Test
  public void defaultConstructor_CreatesEmptyInstance() {
    // Arrange & Act
    BulkOperationResult result = new BulkOperationResult();

    // Assert
    assertNotNull(result);
    assertEquals(0, result.getTotalProcessed());
    assertEquals(0, result.getSuccessCount());
    assertEquals(0, result.getFailedCount());
    assertNull(result.getMessage());
  }

  @Test
  public void setMessage_WithNullMessage_AcceptsNull() {
    // Arrange
    result.setMessage("Initial message");

    // Act
    result.setMessage(null);

    // Assert
    assertNull(result.getMessage());
  }

  @Test
  public void setMessage_WithEmptyMessage_AcceptsEmpty() {
    // Arrange & Act
    result.setMessage("");

    // Assert
    assertEquals("", result.getMessage());
  }

  @Test
  public void setTotalProcessed_WithLargeValue_AcceptsValue() {
    // Arrange & Act
    result.setTotalProcessed(Integer.MAX_VALUE);

    // Assert
    assertEquals(Integer.MAX_VALUE, result.getTotalProcessed());
  }

  @Test
  public void setSuccessCount_WithLargeValue_AcceptsValue() {
    // Arrange & Act
    result.setSuccessCount(Integer.MAX_VALUE);

    // Assert
    assertEquals(Integer.MAX_VALUE, result.getSuccessCount());
  }

  @Test
  public void setFailedCount_WithLargeValue_AcceptsValue() {
    // Arrange & Act
    result.setFailedCount(Integer.MAX_VALUE);

    // Assert
    assertEquals(Integer.MAX_VALUE, result.getFailedCount());
  }

  @Test
  public void setTotalProcessed_WithNegativeValue_AcceptsValue() {
    // Arrange & Act
    result.setTotalProcessed(-1);

    // Assert
    assertEquals(-1, result.getTotalProcessed());
  }

  @Test
  public void dataIntegrity_WithMixedValues_MaintainsCorrectness() {
    // Arrange
    int total = 1000;
    int success = 950;
    int failed = 50;
    String message =
        "Processed %d users for cohort 'test'. Success: %d, Failed: %d"
            .formatted(total, success, failed);

    // Act
    result.setTotalProcessed(total);
    result.setSuccessCount(success);
    result.setFailedCount(failed);
    result.setMessage(message);

    // Assert
    assertEquals(total, result.getTotalProcessed());
    assertEquals(success, result.getSuccessCount());
    assertEquals(failed, result.getFailedCount());
    assertEquals(message, result.getMessage());
    // Verify that success + failed equals total (in this case)
    assertEquals(total, success + failed);
  }
}
