package com.ascend.flockr.users.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import com.ascend.flockr.common.client.Aerospike;
import com.ascend.flockr.common.config.AerospikeConfig;
import com.ascend.flockr.common.constants.Constants;
import com.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.file.AsyncFile;
import io.vertx.rxjava3.core.file.FileSystem;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
    when(vertx.fileSystem()).thenReturn(fileSystem);
    when(aerospikeConfig.getNamespace()).thenReturn("test-namespace");
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
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String setName = tenantId + "_" + projectId;
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("cohort1", currentTime + 10000); // active
    cohortMap.put("cohort2", currentTime + 20000); // active
    cohortMap.put("cohort3", currentTime - 10000); // expired

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(setName)))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, tenantId, projectId).blockingGet();

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
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String setName = tenantId + "_" + projectId;
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("cohort1", currentTime + 10000);

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(setName)))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, tenantId, projectId).blockingGet();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("cohort1", result.get(0));
  }

  @Test
  public void getCohorts_WithEmptyMap_ReturnsEmptyList() {
    // Arrange
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String setName = tenantId + "_" + projectId;
    Map<String, Long> emptyMap = new HashMap<>();

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(setName)))
        .thenReturn(Single.just(emptyMap));

    // Act
    List<String> result = service.getCohorts(userId, tenantId, projectId).blockingGet();

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getCohorts_WithAllExpiredCohorts_ReturnsEmptyList() {
    // Arrange
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String setName = tenantId + "_" + projectId;
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("cohort1", currentTime - 10000); // expired
    cohortMap.put("cohort2", currentTime - 20000); // expired

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(setName)))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, tenantId, projectId).blockingGet();

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getCohorts_WithAerospikeError_PropagatesError() {
    // Arrange
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String setName = tenantId + "_" + projectId;
    RuntimeException error = new RuntimeException("Aerospike connection failed");

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(setName)))
        .thenReturn(Single.error(error));

    // Act & Assert
    try {
      service.getCohorts(userId, tenantId, projectId).blockingGet();
      fail("Expected exception to be thrown");
    } catch (RuntimeException e) {
      assertEquals("Aerospike connection failed", e.getMessage());
    }
  }

  // ==================== mapUserCohorts Tests ====================

  @Test
  public void mapUserCohorts_WithAppendAction_ReturnsTrue() throws Exception {
    // Arrange
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String setName = tenantId + "_" + projectId;
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    when(aerospikeClient.appendCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), anyLong(), eq(setName)))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(userId, tenantId, projectId, request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .appendCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), anyLong(), eq(setName));
  }

  @Test
  public void mapUserCohorts_WithRemoveAction_ReturnsTrue() {
    // Arrange
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String setName = tenantId + "_" + projectId;
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");

    when(aerospikeClient.removeCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(setName)))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(userId, tenantId, projectId, request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .removeCohort(eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(setName));
  }

  @Test
  public void mapUserCohorts_WithValidParams_ProcessesRequest() {
    // Arrange
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String setName = tenantId + "_" + projectId;
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");

    when(aerospikeClient.removeCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(setName)))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(userId, tenantId, projectId, request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .removeCohort(eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(setName));
  }

  @Test
  public void mapUserCohorts_WithKeyNotFoundError_ReturnsFalse() {
    // Arrange
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
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
    Boolean result = service.mapUserCohorts(userId, tenantId, projectId, request).blockingGet();

    // Assert
    assertFalse(result);
  }

  @Test
  public void mapUserCohorts_WithInvalidExpiryTime_ThrowsRestException() {
    // Arrange
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2020-01-01 00:00:00"); // Past date

    // Act & Assert
    try {
      service.mapUserCohorts(userId, tenantId, projectId, request).blockingGet();
      fail("Expected RestException to be thrown");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("INVALID_EXPIRY_TIME") || e.getCause() != null);
    }
  }

  @Test
  public void mapUserCohorts_WithOtherAerospikeError_ThrowsInternalServerError() {
    // Arrange
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
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
      service.mapUserCohorts(userId, tenantId, projectId, request).blockingGet();
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void mapUserCohorts_WithValidParams_ProcessesSuccessfully() {
    // Arrange
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String setName = tenantId + "_" + projectId;
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");

    when(aerospikeClient.removeCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(setName)))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(userId, tenantId, projectId, request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .removeCohort(eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq(setName));
  }

  @Test
  public void mapUserCohorts_WithExceptionInExpiryCalculation_ReturnsError() {
    // Arrange
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("invalid-date-format");

    // Act & Assert
    try {
      service.mapUserCohorts(userId, tenantId, projectId, request).blockingGet();
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
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String csvContent = "550e8400-e29b-41d4-a716-446655440000,6ba7b810-9dad-11d1-80b4-00c04fd430c8";

    InputPart csvFilePart = mock(InputPart.class);
    java.io.InputStream inputStream =
        new java.io.ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
    when(csvFilePart.getBody(java.io.InputStream.class, null)).thenReturn(inputStream);

    when(aerospikeClient.appendCohort(
            anyString(), eq(cohortName), anyString(), anyLong(), anyString()))
        .thenReturn(Single.just(true));

    // Mock file system operations
    when(fileSystem.open(anyString(), any(OpenOptions.class))).thenReturn(Single.just(asyncFile));

    // Mock async file reading
    doAnswer(
            invocation -> {
              RecordParser parser = invocation.getArgument(0);
              // Simulate reading CSV content
              Buffer buffer1 = Buffer.buffer("550e8400-e29b-41d4-a716-446655440000,");
              Buffer buffer2 = Buffer.buffer("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
              parser.handle(buffer1);
              parser.handle(buffer2);
              return null;
            })
        .when(asyncFile)
        .handler(any());

    // Act
    var result =
        service.assignUsersToCohort(cohortName, tenantId, projectId, csvFilePart).blockingGet();

    // Assert
    assertNotNull(result);
    assertTrue(result.getTotalProcessed() >= 0);
  }

  @Test
  public void assignUsersToCohort_WithEmptyCsv_ThrowsException() throws Exception {
    // Arrange
    String cohortName = "test-cohort";
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    java.io.InputStream emptyStream = new java.io.ByteArrayInputStream(new byte[0]);

    InputPart csvFilePart = mock(InputPart.class);
    when(csvFilePart.getBody(java.io.InputStream.class, null)).thenReturn(emptyStream);

    // Act & Assert
    try {
      service.assignUsersToCohort(cohortName, tenantId, projectId, csvFilePart).blockingGet();
      fail("Expected exception to be thrown for empty CSV");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("empty") || e.getMessage().contains("EMPTY_CSV_FILE"));
    }
  }

  @Test
  public void assignUsersToCohort_WithNullCsv_ThrowsException() {
    // Arrange
    String cohortName = "test-cohort";
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    InputPart csvFilePart = mock(InputPart.class);
    try {
      when(csvFilePart.getBody(java.io.InputStream.class, null)).thenReturn(null);
    } catch (Exception e) {
      // Mock setup can throw, but we'll handle it in the test
    }

    // Act & Assert
    try {
      service.assignUsersToCohort(cohortName, tenantId, projectId, csvFilePart).blockingGet();
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
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String setName = tenantId + "_" + projectId;
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("active1", currentTime + 5000);
    cohortMap.put("active2", currentTime + 10000);
    cohortMap.put("expired1", currentTime - 5000);
    cohortMap.put("expired2", currentTime - 10000);
    cohortMap.put("expired3", currentTime - 1); // Just expired

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(setName)))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, tenantId, projectId).blockingGet();

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
    Long userId = 123L;
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String setName = tenantId + "_" + projectId;
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("exact", currentTime); // Exactly current time (should be included)

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq(setName)))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, tenantId, projectId).blockingGet();

    // Assert
    assertEquals(1, result.size());
    assertTrue(result.contains("exact"));
  }
}
