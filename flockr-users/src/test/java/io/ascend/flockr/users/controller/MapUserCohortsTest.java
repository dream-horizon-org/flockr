package io.ascend.flockr.users.controller;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.ascend.flockr.users.constants.Constants;
import io.ascend.flockr.users.dto.BulkOperationResult;
import io.ascend.flockr.users.dto.ResponseEntity;
import io.ascend.flockr.users.dto.request.BatchMapUserCohortsRequest;
import io.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import io.ascend.flockr.users.service.UserCohortsService;
import io.reactivex.rxjava3.core.Single;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link MapUserCohorts} controller.
 *
 * <p>Tests cover request validation, error handling, and response formatting for mapping users to
 * cohorts.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class MapUserCohortsTest {

  @Mock private UserCohortsService userCohortsService;

  @InjectMocks private MapUserCohorts controller;

  @Before
  public void setUp() {
    // Setup is handled by MockitoJUnitRunner
  }

  @Test
  public void handle_WithValidAppendRequest_ReturnsSuccessResponse() throws Exception {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    when(userCohortsService.mapUserCohorts(
            eq(userIdHeader), eq(projectKey), any(MapUserCohortsRequest.class)))
        .thenReturn(Single.just(true));

    // Act
    CompletionStage<ResponseEntity.Success<Boolean>> responseStage =
        controller.handle(userIdHeader, projectKey, request);
    ResponseEntity.Success<Boolean> response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(true, response.data());
    verify(userCohortsService)
        .mapUserCohorts(eq(userIdHeader), eq(projectKey), any(MapUserCohortsRequest.class));
  }

  @Test
  public void handle_WithValidRemoveRequest_ReturnsSuccessResponse() throws Exception {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");

    when(userCohortsService.mapUserCohorts(
            eq(userIdHeader), eq(projectKey), any(MapUserCohortsRequest.class)))
        .thenReturn(Single.just(true));

    // Act
    CompletionStage<ResponseEntity.Success<Boolean>> responseStage =
        controller.handle(userIdHeader, projectKey, request);
    ResponseEntity.Success<Boolean> response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(true, response.data());
  }

  @Test
  public void handle_WithMissingUserIdHeader_ThrowsException() {
    // Arrange
    String userIdHeader = null;
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey, request).toCompletableFuture().get();
      fail("Expected exception to be thrown when userId header is missing");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("MISSING_USER_ID_HEADER")
                  || e.getCause().getMessage().contains("userId")));
    }
  }

  @Test
  public void handle_WithMissingProjectKeyHeader_ThrowsException() {
    // Arrange
    String userIdHeader = "123";
    String projectKey = null;
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey, request).toCompletableFuture().get();
      fail("Expected exception to be thrown when x-project-key header is missing");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("MISSING_PROJECT_KEY_HEADER")
                  || e.getCause().getMessage().contains("x-project-key")));
    }
  }

  @Test
  public void handle_WithNullRequest_ThrowsException() {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = null;

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey, request).toCompletableFuture().get();
      fail("Expected exception to be thrown for null request");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_REQUEST")
                  || e.getCause().getMessage().contains("Request body")));
    }
  }

  @Test
  public void handle_WithMissingCohortKey_ThrowsException() {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey(null); // Missing cohortKey
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey, request).toCompletableFuture().get();
      fail("Expected exception to be thrown for missing cohort_key");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("MISSING_COHORT_KEY")
                  || e.getCause().getMessage().contains("cohort_key")));
    }
  }

  @Test
  public void handle_WithMissingAction_ThrowsException() {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(null); // Missing action
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey, request).toCompletableFuture().get();
      fail("Expected exception to be thrown for missing action");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("MISSING_ACTION")
                  || e.getCause().getMessage().contains("action")));
    }
  }

  @Test
  public void handle_WithMissingExpireAt_ThrowsException() {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt(null); // Missing expireAt

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey, request).toCompletableFuture().get();
      fail("Expected exception to be thrown for missing expire_at");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("MISSING_EXPIRE_AT")
                  || e.getCause().getMessage().contains("expire_at")));
    }
  }

  @Test
  public void handle_WithInvalidAction_ThrowsException() {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction("invalid-action"); // Invalid action
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey, request).toCompletableFuture().get();
      fail("Expected exception to be thrown for invalid action");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_ACTION")
                  || e.getCause().getMessage().contains("action")));
    }
  }

  @Test
  public void handle_WithServiceReturningFalse_ReturnsFalseInResponse() throws Exception {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    when(userCohortsService.mapUserCohorts(
            eq(userIdHeader), eq(projectKey), any(MapUserCohortsRequest.class)))
        .thenReturn(Single.just(false));

    // Act
    CompletionStage<ResponseEntity.Success<Boolean>> responseStage =
        controller.handle(userIdHeader, projectKey, request);
    ResponseEntity.Success<Boolean> response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(false, response.data());
  }

  @Test
  public void handle_WithServiceError_PropagatesError() {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    RuntimeException serviceError = new RuntimeException("Service error");
    when(userCohortsService.mapUserCohorts(
            eq(userIdHeader), eq(projectKey), any(MapUserCohortsRequest.class)))
        .thenReturn(Single.error(serviceError));

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey, request).toCompletableFuture().get();
      fail("Expected exception to be propagated");
    } catch (Exception e) {
      assertNotNull(e);
      // The error should be propagated from the service
    }
  }

  // ==================== handleBatch Tests ====================

  @Test
  public void handleBatch_WithValidRequests_ReturnsSuccessResponse() throws Exception {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    List<BatchMapUserCohortsRequest> requests =
        Arrays.asList(
            new BatchMapUserCohortsRequest(
                123L, "cohort1", Constants.ACTION_APPEND, "2025-12-31 23:59:59"),
            new BatchMapUserCohortsRequest(
                456L, "cohort2", Constants.ACTION_REMOVE, "2025-12-31 23:59:59"));

    BulkOperationResult expectedResult =
        new BulkOperationResult(2, 2, 0, "Processed 2 users successfully");

    when(userCohortsService.batchMapUserCohorts(eq(projectKey), anyList()))
        .thenReturn(Single.just(expectedResult));

    // Act
    CompletionStage<ResponseEntity.Success<BulkOperationResult>> responseStage =
        controller.handleBatch(projectKey, requests);
    ResponseEntity.Success<BulkOperationResult> response =
        responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(2, response.data().getTotalProcessed());
    assertEquals(2, response.data().getSuccessCount());
    assertEquals(0, response.data().getFailedCount());
  }

  @Test
  public void handleBatch_WithEmptyList_ReturnsEmptyResult() throws Exception {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    List<BatchMapUserCohortsRequest> requests = Collections.emptyList();

    BulkOperationResult expectedResult = new BulkOperationResult(0, 0, 0, "No users to process");

    when(userCohortsService.batchMapUserCohorts(eq(projectKey), anyList()))
        .thenReturn(Single.just(expectedResult));

    // Act
    CompletionStage<ResponseEntity.Success<BulkOperationResult>> responseStage =
        controller.handleBatch(projectKey, requests);
    ResponseEntity.Success<BulkOperationResult> response =
        responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(0, response.data().getTotalProcessed());
  }

  @Test
  public void handleBatch_WithMissingProjectKeyHeader_ThrowsException() {
    // Arrange
    String projectKey = null;
    List<BatchMapUserCohortsRequest> requests =
        Arrays.asList(
            new BatchMapUserCohortsRequest(
                123L, "cohort1", Constants.ACTION_APPEND, "2025-12-31 23:59:59"));

    // Act & Assert
    try {
      controller.handleBatch(projectKey, requests).toCompletableFuture().get();
      fail("Expected exception to be thrown for missing x-project-key header");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("MISSING_PROJECT_KEY_HEADER")
                  || e.getCause().getMessage().contains("x-project-key")));
    }
  }

  @Test
  public void handleBatch_WithServiceError_PropagatesError() {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    List<BatchMapUserCohortsRequest> requests =
        Arrays.asList(
            new BatchMapUserCohortsRequest(
                123L, "cohort1", Constants.ACTION_APPEND, "2025-12-31 23:59:59"));

    RuntimeException serviceError = new RuntimeException("Batch processing error");
    when(userCohortsService.batchMapUserCohorts(eq(projectKey), anyList()))
        .thenReturn(Single.error(serviceError));

    // Act & Assert
    try {
      controller.handleBatch(projectKey, requests).toCompletableFuture().get();
      fail("Expected exception to be propagated");
    } catch (Exception e) {
      assertNotNull(e);
      // Error should be mapped to RestException by ErrorHandler
    }
  }

  @Test
  public void handleBatch_WithPartialFailures_ReturnsPartialResult() throws Exception {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    List<BatchMapUserCohortsRequest> requests =
        Arrays.asList(
            new BatchMapUserCohortsRequest(
                123L, "cohort1", Constants.ACTION_APPEND, "2025-12-31 23:59:59"),
            new BatchMapUserCohortsRequest(
                456L, "cohort2", Constants.ACTION_APPEND, "2025-12-31 23:59:59"));

    BulkOperationResult expectedResult =
        new BulkOperationResult(2, 1, 1, "Processed 2 users. Success: 1, Failed: 1");

    when(userCohortsService.batchMapUserCohorts(eq(projectKey), anyList()))
        .thenReturn(Single.just(expectedResult));

    // Act
    CompletionStage<ResponseEntity.Success<BulkOperationResult>> responseStage =
        controller.handleBatch(projectKey, requests);
    ResponseEntity.Success<BulkOperationResult> response =
        responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(2, response.data().getTotalProcessed());
    assertEquals(1, response.data().getSuccessCount());
    assertEquals(1, response.data().getFailedCount());
  }
}
