package com.ascend.flockr.users.controller;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ascend.flockr.users.dto.ResponseEntity;
import com.ascend.flockr.users.service.UserCohortsService;
import io.reactivex.rxjava3.core.Single;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.CompletionStage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link GetUserCohorts} controller.
 *
 * <p>Tests cover parameter validation, error handling, and response formatting for retrieving user
 * cohorts.
 *
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class GetUserCohortsTest {

  @Mock private UserCohortsService userCohortsService;

  @InjectMocks private GetUserCohorts controller;

  @Before
  public void setUp() {
    // Setup is handled by MockitoJUnitRunner
  }

  @Test
  public void handle_WithValidUserId_ReturnsSuccessResponse() throws Exception {
    // Arrange
    Long userId = 123L;
    Long projectId = 100L;
    List<String> cohorts = Arrays.asList("cohort1", "cohort2", "cohort3");

    when(userCohortsService.getCohorts(eq(userId), isNull(), eq(projectId)))
        .thenReturn(Single.just(cohorts));

    // Act
    CompletionStage<Response> responseStage = controller.handle(userId, null, projectId);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof ResponseEntity.Success);
    ResponseEntity.Success<?> success = (ResponseEntity.Success<?>) response.getEntity();
    assertEquals(cohorts, success.data());
  }

  @Test
  public void handle_WithValidGuestId_ReturnsSuccessResponse() throws Exception {
    // Arrange
    String guestId = "guest-123";
    Long projectId = 100L;
    List<String> cohorts = Arrays.asList("cohort1", "cohort2");

    when(userCohortsService.getCohorts(isNull(), eq(guestId), eq(projectId)))
        .thenReturn(Single.just(cohorts));

    // Act
    CompletionStage<Response> responseStage = controller.handle(null, guestId, projectId);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof ResponseEntity.Success);
  }

  @Test
  public void handle_WithEmptyCohortsList_ReturnsEmptyList() throws Exception {
    // Arrange
    Long userId = 123L;
    Long projectId = 100L;
    List<String> emptyCohorts = Collections.emptyList();

    when(userCohortsService.getCohorts(eq(userId), isNull(), eq(projectId)))
        .thenReturn(Single.just(emptyCohorts));

    // Act
    CompletionStage<Response> responseStage = controller.handle(userId, null, projectId);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ResponseEntity.Success<?> success = (ResponseEntity.Success<?>) response.getEntity();
    assertEquals(emptyCohorts, success.data());
  }

  @Test
  public void handle_WithNullProjectId_ThrowsException() {
    // Arrange
    Long userId = 123L;
    Long projectId = null;

    // Act & Assert
    try {
      controller.handle(userId, null, projectId).toCompletableFuture().get();
      fail("Expected exception to be thrown for null projectId");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_REQUEST_PARAMS")
                  || e.getCause().getMessage().contains("projectId")));
    }
  }

  @Test
  public void handle_WithZeroProjectId_ThrowsException() {
    // Arrange
    Long userId = 123L;
    Long projectId = 0L;

    // Act & Assert
    try {
      controller.handle(userId, null, projectId).toCompletableFuture().get();
      fail("Expected exception to be thrown for zero projectId");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_REQUEST_PARAMS")
                  || e.getCause().getMessage().contains("projectId")));
    }
  }

  @Test
  public void handle_WithNegativeProjectId_ThrowsException() {
    // Arrange
    Long userId = 123L;
    Long projectId = -1L;

    // Act & Assert
    try {
      controller.handle(userId, null, projectId).toCompletableFuture().get();
      fail("Expected exception to be thrown for negative projectId");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_REQUEST_PARAMS")
                  || e.getCause().getMessage().contains("projectId")));
    }
  }

  @Test
  public void handle_WithBothUserIdAndGuestIdNull_ThrowsException() {
    // Arrange
    Long userId = null;
    String guestId = null;
    Long projectId = 100L;

    // Act & Assert
    try {
      controller.handle(userId, guestId, projectId).toCompletableFuture().get();
      fail("Expected exception to be thrown when both userId and guestId are null");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_REQUEST_PARAMS")
                  || e.getCause().getMessage().contains("userId")
                  || e.getCause().getMessage().contains("guestId")));
    }
  }

  @Test
  public void handle_WithNegativeUserId_ThrowsException() {
    // Arrange
    Long userId = -1L;
    Long projectId = 100L;

    // Act & Assert
    try {
      controller.handle(userId, null, projectId).toCompletableFuture().get();
      fail("Expected exception to be thrown for negative userId");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_REQUEST_PARAMS")
                  || e.getCause().getMessage().contains("userId")));
    }
  }

  @Test
  public void handle_WithZeroUserId_ThrowsException() {
    // Arrange
    Long userId = 0L;
    Long projectId = 100L;

    // Act & Assert
    try {
      controller.handle(userId, null, projectId).toCompletableFuture().get();
      fail("Expected exception to be thrown for zero userId");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_REQUEST_PARAMS")
                  || e.getCause().getMessage().contains("userId")));
    }
  }

  @Test
  public void handle_WithServiceError_PropagatesError() {
    // Arrange
    Long userId = 123L;
    Long projectId = 100L;
    RuntimeException serviceError = new RuntimeException("Service error");

    when(userCohortsService.getCohorts(eq(userId), isNull(), eq(projectId)))
        .thenReturn(Single.error(serviceError));

    // Act & Assert
    try {
      controller.handle(userId, null, projectId).toCompletableFuture().get();
      fail("Expected exception to be propagated");
    } catch (Exception e) {
      assertNotNull(e);
      // The error should be propagated from the service
    }
  }

  @Test
  public void handle_WithBothUserIdAndGuestIdProvided_UsesUserId() throws Exception {
    // Arrange
    Long userId = 123L;
    String guestId = "guest-123";
    Long projectId = 100L;
    List<String> cohorts = Arrays.asList("cohort1");

    // When both are provided, userId takes precedence
    when(userCohortsService.getCohorts(eq(userId), eq(guestId), eq(projectId)))
        .thenReturn(Single.just(cohorts));

    // Act
    CompletionStage<Response> responseStage = controller.handle(userId, guestId, projectId);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(userCohortsService).getCohorts(eq(userId), eq(guestId), eq(projectId));
  }
}

