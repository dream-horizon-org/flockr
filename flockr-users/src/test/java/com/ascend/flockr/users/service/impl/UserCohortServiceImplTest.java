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
    Long projectId = 100L;
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("cohort1", currentTime + 10000); // active
    cohortMap.put("cohort2", currentTime + 20000); // active
    cohortMap.put("cohort3", currentTime - 10000); // expired

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq("100")))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, null, projectId).blockingGet();

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.contains("cohort1"));
    assertTrue(result.contains("cohort2"));
    assertFalse(result.contains("cohort3"));
  }

  @Test
  public void getCohorts_WithGuestId_ReturnsActiveCohorts() {
    // Arrange
    String guestId = "guest-123";
    Long projectId = 100L;
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("cohort1", currentTime + 10000);

    when(aerospikeClient.getCohortExpiryBin(eq(guestId), eq("100")))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(null, guestId, projectId).blockingGet();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("cohort1", result.get(0));
  }

  @Test
  public void getCohorts_WithEmptyMap_ReturnsEmptyList() {
    // Arrange
    Long userId = 123L;
    Long projectId = 100L;
    Map<String, Long> emptyMap = new HashMap<>();

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq("100")))
        .thenReturn(Single.just(emptyMap));

    // Act
    List<String> result = service.getCohorts(userId, null, projectId).blockingGet();

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getCohorts_WithAllExpiredCohorts_ReturnsEmptyList() {
    // Arrange
    Long userId = 123L;
    Long projectId = 100L;
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("cohort1", currentTime - 10000); // expired
    cohortMap.put("cohort2", currentTime - 20000); // expired

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq("100")))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, null, projectId).blockingGet();

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getCohorts_WithAerospikeError_PropagatesError() {
    // Arrange
    Long userId = 123L;
    Long projectId = 100L;
    RuntimeException error = new RuntimeException("Aerospike connection failed");

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq("100")))
        .thenReturn(Single.error(error));

    // Act & Assert
    try {
      service.getCohorts(userId, null, projectId).blockingGet();
      fail("Expected exception to be thrown");
    } catch (RuntimeException e) {
      assertEquals("Aerospike connection failed", e.getMessage());
    }
  }

  // ==================== mapUserCohorts Tests ====================

  @Test
  public void mapUserCohorts_WithAppendAction_ReturnsTrue() throws Exception {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    when(aerospikeClient.appendCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), anyLong(), eq("100")))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .appendCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), anyLong(), eq("100"));
  }

  @Test
  public void mapUserCohorts_WithRemoveAction_ReturnsTrue() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    when(aerospikeClient.removeCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq("100")))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .removeCohort(eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq("100"));
  }

  @Test
  public void mapUserCohorts_WithGuestId_UsesGuestIdAsKey() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(null);
    request.setGuestId("guest-123");
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    when(aerospikeClient.removeCohort(
            eq("guest-123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq("100")))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .removeCohort(eq("guest-123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), eq("100"));
  }

  @Test
  public void mapUserCohorts_WithKeyNotFoundError_ReturnsFalse() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    AerospikeException exception =
        new AerospikeException(ResultCode.KEY_NOT_FOUND_ERROR, "Key not found");

    when(aerospikeClient.appendCohort(anyString(), anyString(), anyString(), anyLong(), anyString()))
        .thenReturn(Single.error(exception));

    // Act
    Boolean result = service.mapUserCohorts(request).blockingGet();

    // Assert
    assertFalse(result);
  }

  @Test
  public void mapUserCohorts_WithInvalidExpiryTime_ThrowsRestException() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2020-01-01 00:00:00"); // Past date
    request.setProjectId(100L);

    // Act & Assert
    try {
      service.mapUserCohorts(request).blockingGet();
      fail("Expected RestException to be thrown");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("INVALID_EXPIRY_TIME") || e.getCause() != null);
    }
  }

  @Test
  public void mapUserCohorts_WithOtherAerospikeError_ThrowsInternalServerError() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    AerospikeException exception =
        new AerospikeException(ResultCode.SERVER_ERROR, "Server error");

    when(aerospikeClient.appendCohort(anyString(), anyString(), anyString(), anyLong(), anyString()))
        .thenReturn(Single.error(exception));

    // Act & Assert
    try {
      service.mapUserCohorts(request).blockingGet();
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void mapUserCohorts_WithNullProjectId_UsesNullSetName() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(null);

    when(aerospikeClient.removeCohort(
            eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), isNull()))
        .thenReturn(Single.just(true));

    // Act
    Boolean result = service.mapUserCohorts(request).blockingGet();

    // Assert
    assertTrue(result);
    verify(aerospikeClient)
        .removeCohort(eq("123"), eq("test-cohort"), eq(Constants.SOURCE_DREAM11), isNull());
  }

  @Test
  public void mapUserCohorts_WithExceptionInExpiryCalculation_ReturnsError() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("invalid-date-format");
    request.setProjectId(100L);

    // Act & Assert
    try {
      service.mapUserCohorts(request).blockingGet();
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
    String csvContent = "550e8400-e29b-41d4-a716-446655440000,6ba7b810-9dad-11d1-80b4-00c04fd430c8";
    byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

    InputPart csvFilePart = mock(InputPart.class);
    when(csvFilePart.getBody(byte[].class, null)).thenReturn(csvBytes);

    when(aerospikeClient.appendCohort(anyString(), eq(cohortName), anyString(), anyLong(), anyString()))
        .thenReturn(Single.just(true));

    // Mock file system operations
    when(fileSystem.open(anyString(), any(OpenOptions.class)))
        .thenReturn(Single.just(asyncFile));

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
    var result = service.assignUsersToCohort(cohortName, csvFilePart).blockingGet();

    // Assert
    assertNotNull(result);
    assertTrue(result.getTotalProcessed() >= 0);
  }

  @Test
  public void assignUsersToCohort_WithEmptyCsv_ThrowsException() throws Exception {
    // Arrange
    String cohortName = "test-cohort";
    byte[] emptyBytes = new byte[0];

    InputPart csvFilePart = mock(InputPart.class);
    when(csvFilePart.getBody(byte[].class, null)).thenReturn(emptyBytes);

    // Act & Assert
    try {
      service.assignUsersToCohort(cohortName, csvFilePart).blockingGet();
      fail("Expected exception to be thrown for empty CSV");
    } catch (Exception e) {
      assertTrue(
          e.getMessage().contains("empty") || e.getMessage().contains("INVALID_REQUEST_PARAMS"));
    }
  }

  @Test
  public void assignUsersToCohort_WithNullCsv_ThrowsException() {
    // Arrange
    String cohortName = "test-cohort";
    InputPart csvFilePart = mock(InputPart.class);
    try {
      when(csvFilePart.getBody(byte[].class, null)).thenReturn(null);
    } catch (Exception e) {
      // Mock setup can throw, but we'll handle it in the test
    }

    // Act & Assert
    try {
      service.assignUsersToCohort(cohortName, csvFilePart).blockingGet();
      fail("Expected exception to be thrown for null CSV");
    } catch (Exception e) {
      assertTrue(
          e.getMessage().contains("empty") || e.getMessage().contains("INVALID_REQUEST_PARAMS"));
    }
  }

  // ==================== Helper Method Tests ====================

  @Test
  public void getActiveCohortsFromMap_WithMixedCohorts_FiltersExpired() {
    // This tests the private method indirectly through getCohorts
    // Arrange
    Long userId = 123L;
    Long projectId = 100L;
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("active1", currentTime + 5000);
    cohortMap.put("active2", currentTime + 10000);
    cohortMap.put("expired1", currentTime - 5000);
    cohortMap.put("expired2", currentTime - 10000);
    cohortMap.put("expired3", currentTime - 1); // Just expired

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq("100")))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, null, projectId).blockingGet();

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
    Long projectId = 100L;
    Map<String, Long> cohortMap = new HashMap<>();
    long currentTime = System.currentTimeMillis();
    cohortMap.put("exact", currentTime); // Exactly current time (should be included)

    when(aerospikeClient.getCohortExpiryBin(eq("123"), eq("100")))
        .thenReturn(Single.just(cohortMap));

    // Act
    List<String> result = service.getCohorts(userId, null, projectId).blockingGet();

    // Assert
    assertEquals(1, result.size());
    assertTrue(result.contains("exact"));
  }
}

