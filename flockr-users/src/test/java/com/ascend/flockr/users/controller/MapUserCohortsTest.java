package com.ascend.flockr.users.controller;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ascend.flockr.common.constants.Constants;
import com.ascend.flockr.users.dto.ResponseEntity;
import com.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import com.ascend.flockr.users.service.UserCohortsService;
import io.reactivex.rxjava3.core.Single;
import jakarta.ws.rs.core.Response;
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
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    when(userCohortsService.mapUserCohorts(any(MapUserCohortsRequest.class)))
        .thenReturn(Single.just(true));

    // Act
    CompletionStage<Response> responseStage = controller.handle(request);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof ResponseEntity.Success);
    ResponseEntity.Success<?> success = (ResponseEntity.Success<?>) response.getEntity();
    assertEquals(true, success.data());
    verify(userCohortsService).mapUserCohorts(any(MapUserCohortsRequest.class));
  }

  @Test
  public void handle_WithValidRemoveRequest_ReturnsSuccessResponse() throws Exception {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    when(userCohortsService.mapUserCohorts(any(MapUserCohortsRequest.class)))
        .thenReturn(Single.just(true));

    // Act
    CompletionStage<Response> responseStage = controller.handle(request);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(response.getEntity() instanceof ResponseEntity.Success);
  }

  @Test
  public void handle_WithGuestId_ReturnsSuccessResponse() throws Exception {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(null);
    request.setGuestId("guest-123");
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    when(userCohortsService.mapUserCohorts(any(MapUserCohortsRequest.class)))
        .thenReturn(Single.just(true));

    // Act
    CompletionStage<Response> responseStage = controller.handle(request);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(userCohortsService).mapUserCohorts(any(MapUserCohortsRequest.class));
  }

  @Test
  public void handle_WithBothUserIdAndGuestIdNull_ThrowsException() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(null);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    // Act & Assert
    try {
      controller.handle(request).toCompletableFuture().get();
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
  public void handle_WithInvalidRequest_ThrowsException() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    // Missing required fields - this should fail validation
    request.setUserId(123L);
    request.setCohortKey(null); // Missing cohortKey
    request.setSource(null); // Missing source
    request.setAction(null); // Missing action
    request.setExpireAt(null); // Missing expireAt

    // Act & Assert
    try {
      controller.handle(request).toCompletableFuture().get();
      fail("Expected exception to be thrown for invalid request");
    } catch (Exception e) {
      assertNotNull(e);
      // Validation should fail
    }
  }

  @Test
  public void handle_WithServiceReturningFalse_ReturnsFalseInResponse() throws Exception {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    when(userCohortsService.mapUserCohorts(any(MapUserCohortsRequest.class)))
        .thenReturn(Single.just(false));

    // Act
    CompletionStage<Response> responseStage = controller.handle(request);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ResponseEntity.Success<?> success = (ResponseEntity.Success<?>) response.getEntity();
    assertEquals(false, success.data());
  }

  @Test
  public void handle_WithServiceError_PropagatesError() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    RuntimeException serviceError = new RuntimeException("Service error");
    when(userCohortsService.mapUserCohorts(any(MapUserCohortsRequest.class)))
        .thenReturn(Single.error(serviceError));

    // Act & Assert
    try {
      controller.handle(request).toCompletableFuture().get();
      fail("Expected exception to be propagated");
    } catch (Exception e) {
      assertNotNull(e);
      // The error should be propagated from the service
    }
  }

  @Test
  public void handle_WithNullProjectId_ProcessesRequest() throws Exception {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey("test-cohort");
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_REMOVE);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(null); // null projectId is allowed

    when(userCohortsService.mapUserCohorts(any(MapUserCohortsRequest.class)))
        .thenReturn(Single.just(true));

    // Act
    CompletionStage<Response> responseStage = controller.handle(request);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(userCohortsService).mapUserCohorts(any(MapUserCohortsRequest.class));
  }

  @Test
  public void handle_WithValidationFailure_ThrowsException() {
    // Arrange
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setUserId(123L);
    request.setGuestId(null);
    request.setCohortKey(""); // Empty cohortKey should fail validation
    request.setSource(Constants.SOURCE_DREAM11);
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");
    request.setProjectId(100L);

    // Act & Assert
    try {
      controller.handle(request).toCompletableFuture().get();
      fail("Expected exception to be thrown for validation failure");
    } catch (Exception e) {
      assertNotNull(e);
      // Validation should fail
    }
  }
}

