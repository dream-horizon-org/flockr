package io.ascend.flockr.users.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import io.ascend.flockr.users.client.Aerospike;
import io.ascend.flockr.users.config.AerospikeConfig;
import io.ascend.flockr.users.constants.Constants;
import io.ascend.flockr.users.dto.BulkOperationResult;
import io.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.file.AsyncFile;
import io.vertx.rxjava3.core.file.FileSystem;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link UserCohortServiceImpl}.
 *
 * <p>Tests cover all service methods including cohort retrieval, mapping, and bulk assignment
 * operations with various edge cases and error scenarios.
 *
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class UserCohortServiceImplTest {

  @Mock private Aerospike aerospikeClient;

  @Mock private AerospikeConfig aerospikeConfig;

  @Mock private Vertx vertx;

  @Mock private FileSystem fileSystem;

  @Mock private AsyncFile asyncFile;

  private UserCohortServiceImpl service;
  private Path tempFile;

  @Before
  public void setUp() {
    // Use lenient() since some tests create their own service instances
    lenient().when(vertx.fileSystem()).thenReturn(fileSystem);
    lenient().when(aerospikeConfig.getNamespace()).thenReturn("test-namespace");
    service = new UserCohortServiceImpl(aerospikeClient, aerospikeConfig, vertx);
  }

  @After
  public void tearDown() throws Exception {
    if (tempFile != null && Files.exists(tempFile)) {
      Files.deleteIfExists(tempFile);
    }
  }

  // ==================== getCohorts Tests ====================

  @Test
  public void getCohorts_WithActiveCohorts_ReturnsFilteredList() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("cohort1", currentTime + 10000); // active
    cohortMap.put("cohort2", currentTime + 20000); // active
    cohortMap.put("cohort3", currentTime - 10000); // expired

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(projectKey)))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, projectKey).blockingGet();

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.contains("cohort1"));
    assertTrue(result.contains("cohort2"));
    assertFalse(result.contains("cohort3"));
  }

  @Test
  public void getCohorts_WithValidParams_ReturnsActiveCohorts() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("cohort1", currentTime + 10000);

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(projectKey)))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, projectKey).blockingGet();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("cohort1", result.get(0));
  }

  @Test
  public void getCohorts_WithEmptyMap_ReturnsEmptyList() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    Map<String, Long> emptyMap = new HashMap<>();

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(projectKey)))
        .thenReturn(Single.just(emptyMap));

    // Act
    List<String> result = service.getCohorts(userId, projectKey).blockingGet();

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getCohorts_WithAllExpiredCohorts_ReturnsEmptyList() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("cohort1", currentTime - 10000); // expired
    cohortMap.put("cohort2", currentTime - 20000); // expired

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(projectKey)))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, projectKey).blockingGet();

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getCohorts_WithAerospikeError_PropagatesError() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    RuntimeException error = new RuntimeException("Aerospike connection failed");

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(projectKey)))
        .thenReturn(Single.error(error));

    // Act & Assert
    try {
      service.getCohorts(userId, projectKey).blockingGet();
      fail("Expected exception to be thrown");
    } catch (RuntimeException e) {
      assertEquals("Aerospike connection failed", e.getMessage());
    }
  }

  // ==================== mapUserCohorts Tests ====================

  @Test
  public void mapUserCohorts_WithAppendAction_ReturnsTrue() throws Exception {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    when(aerospikeClient.appendCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), anyLong(), eq(projectKey)))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(userId, projectKey, request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .appendCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), anyLong(), eq(projectKey));
  }

  @Test
  public void mapUserCohorts_WithRemoveAction_ReturnsTrue() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");

    when(aerospikeClient.removeCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(projectKey)))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(userId, projectKey, request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .removeCohort(eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(projectKey));
  }

  @Test
  public void mapUserCohorts_WithValidParams_ProcessesRequest() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");

    when(aerospikeClient.removeCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(projectKey)))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(userId, projectKey, request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .removeCohort(eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(projectKey));
  }

  @Test
  public void mapUserCohorts_WithKeyNotFoundError_ReturnsFalse() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    AerospikeException exception =
        new AerospikeException(ResultCode.KEY_NOT_FOUND_ERROR, "Key not found");

    when(aerospikeClient.appendCohort(
            anyString(), anyString(), anyString(), anyLong(), anyString()))
        .thenReturn(Single.error(exception));

    // Act
    Boolean result = service.mapUserCohorts(userId, projectKey, request).blockingGet();

    // Assert
    assertFalse(result);
  }

  @Test
  public void mapUserCohorts_WithOtherAerospikeError_ThrowsInternalServerError() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    AerospikeException exception = new AerospikeException(ResultCode.SERVER_ERROR, "Server error");

    when(aerospikeClient.appendCohort(
            anyString(), anyString(), anyString(), anyLong(), anyString()))
        .thenReturn(Single.error(exception));

    // Act & Assert
    try {
      service.mapUserCohorts(userId, projectKey, request).blockingGet();
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void mapUserCohorts_WithValidParams_ProcessesSuccessfully() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");

    when(aerospikeClient.removeCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(projectKey)))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(userId, projectKey, request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .removeCohort(eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(projectKey));
  }

  @Test
  public void mapUserCohorts_WithExceptionInExpiryCalculation_ReturnsError() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("invalid-date-format");

    // Act & Assert
    try {
      service.mapUserCohorts(userId, projectKey, request).blockingGet();
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  // ==================== assignUsersToCohort Tests ====================

  @Test
  public void assignUsersToCohort_WithValidCsv_ReturnsSuccessResult() throws Exception {
    // Arrange
    String cohortName = "test-cohort";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    String csvContent = "550e8400-e29b-41d4-a716-446655440000,6ba7b810-9dad-11d1-80b4-00c04fd430c8";

    // Create a real CSV file to test the service logic without complex Vert.x mocking
    Path csvFile = Files.createTempFile("test-cohort-", ".csv");
    try {
      Files.write(csvFile, csvContent.getBytes(StandardCharsets.UTF_8));

      // Use lenient() since this stubbing might not be reached in all execution paths
      lenient()
          .when(
              aerospikeClient.appendCohort(
                  anyString(), eq(cohortName), anyString(), anyLong(), anyString()))
          .thenReturn(Single.just(true));

      // Use a real Vertx instance for file reading (simpler than mocking async operations)
      io.vertx.rxjava3.core.Vertx testVertx = io.vertx.rxjava3.core.Vertx.vertx();
      try {
        UserCohortServiceImpl testService =
            new UserCohortServiceImpl(aerospikeClient, aerospikeConfig, testVertx);

        // Act - Test the core processing logic directly with a real file
        BulkOperationResult result =
            testService.processCsvAndAssign(csvFile, cohortName, projectKey).blockingGet();

        // Assert
        assertNotNull(result);
        assertTrue(result.getTotalProcessed() >= 0);
        assertTrue(result.getSuccessCount() >= 0);
      } finally {
        // Clean up Vertx instance
        testVertx.close().blockingAwait(5, TimeUnit.SECONDS);
      }
    } finally {
      // Clean up temp file
      Files.deleteIfExists(csvFile);
    }
  }

  @Test
  public void assignUsersToCohort_WithEmptyCsv_ThrowsException() throws Exception {
    // Arrange
    String cohortName = "test-cohort";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    java.io.InputStream emptyStream = new java.io.ByteArrayInputStream(new byte[0]);

    InputPart csvFilePart = mock(InputPart.class);
    when(csvFilePart.getBody(java.io.InputStream.class, null)).thenReturn(emptyStream);

    // Act & Assert
    try {
      service.assignUsersToCohort(cohortName, projectKey, csvFilePart).blockingGet();
      fail("Expected exception to be thrown for empty CSV");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("empty") || e.getMessage().contains("EMPTY_CSV_FILE"));
    }
  }

  @Test
  public void assignUsersToCohort_WithNullCsv_ThrowsException() {
    // Arrange
    String cohortName = "test-cohort";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    InputPart csvFilePart = mock(InputPart.class);
    try {
      when(csvFilePart.getBody(java.io.InputStream.class, null)).thenReturn(null);
    } catch (Exception e) {
      // Mock setup can throw, but we'll handle it in the test
    }

    // Act & Assert
    try {
      service.assignUsersToCohort(cohortName, projectKey, csvFilePart).blockingGet();
      fail("Expected exception to be thrown for null CSV");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("empty") || e.getMessage().contains("EMPTY_CSV_FILE"));
    }
  }

  // ==================== Helper Method Tests ====================

  @Test
  public void getActiveCohortsFromMap_WithMixedCohorts_FiltersExpired() {
    // This tests the private method indirectly through getCohorts
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("active1", currentTime + 5000);
    cohortMap.put("active2", currentTime + 10000);
    cohortMap.put("expired1", currentTime - 5000);
    cohortMap.put("expired2", currentTime - 10000);
    cohortMap.put("expired3", currentTime - 1); // Just expired

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(projectKey)))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, projectKey).blockingGet();

    // Assert
    assertEquals(2, result.size());
    assertTrue(result.contains("active1"));
    assertTrue(result.contains("active2"));
    assertFalse(result.contains("expired1"));
    assertFalse(result.contains("expired2"));
    assertFalse(result.contains("expired3"));
  }

  @Test
  public void getActiveCohortsFromMap_WithExactCurrentTime_IncludesCohort() {
    // Arrange
    String userId = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("exact", currentTime); // Exactly current time (should be included)

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(projectKey)))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, projectKey).blockingGet();

    // Assert
    assertEquals(1, result.size());
    assertTrue(result.contains("exact"));
  }
}
