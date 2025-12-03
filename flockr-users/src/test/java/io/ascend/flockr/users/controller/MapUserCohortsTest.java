package io.ascend.flockr.users.controller;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.ascend.flockr.users.constants.Constants;
import io.ascend.flockr.users.dto.ResponseEntity;
import io.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import io.ascend.flockr.users.service.UserCohortsService;
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
            eq(123L), eq(projectKey), any(MapUserCohortsRequest.class)))
        .thenReturn(Single.just(true));

    // Act
    CompletionStage<Response> responseStage = controller.handle(userIdHeader, projectKey, request);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof ResponseEntity.Success);
    ResponseEntity.Success<?> success = (ResponseEntity.Success<?>) response.getEntity();
    assertEquals(true, success.data());
    verify(userCohortsService)
        .mapUserCohorts(eq(123L), eq(projectKey), any(MapUserCohortsRequest.class));
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
            eq(123L), eq(projectKey), any(MapUserCohortsRequest.class)))
        .thenReturn(Single.just(true));

    // Act
    CompletionStage<Response> responseStage = controller.handle(userIdHeader, projectKey, request);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(response.getEntity() instanceof ResponseEntity.Success);
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
  public void handle_WithInvalidProjectKeyFormat_ThrowsException() {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "invalid-format"; // Missing underscore
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey, request).toCompletableFuture().get();
      fail("Expected exception to be thrown for invalid x-project-key format");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_PROJECT_KEY_FORMAT")
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
            eq(123L), eq(projectKey), any(MapUserCohortsRequest.class)))
        .thenReturn(Single.just(false));

    // Act
    CompletionStage<Response> responseStage = controller.handle(userIdHeader, projectKey, request);
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
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    RuntimeException serviceError = new RuntimeException("Service error");
    when(userCohortsService.mapUserCohorts(
            eq(123L), eq(projectKey), any(MapUserCohortsRequest.class)))
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

  @Test
  public void handle_WithInvalidTenantIdFormat_ThrowsException() {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "invalid-uuid_project-100"; // Invalid UUID format
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey, request).toCompletableFuture().get();
      fail("Expected exception to be thrown for invalid tenantId format");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_TENANT_ID_FORMAT")
                  || e.getCause().getMessage().contains("UUID")));
    }
  }

  @Test
  public void handle_WithInvalidUserId_ThrowsException() {
    // Arrange
    String userIdHeader = "-1";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    MapUserCohortsRequest request = new MapUserCohortsRequest();
    request.setCohortKey("test-cohort");
    request.setAction(Constants.ACTION_APPEND);
    request.setExpireAt("2025-12-31 23:59:59");

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey, request).toCompletableFuture().get();
      fail("Expected exception to be thrown for invalid userId");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_USER_ID")
                  || e.getCause().getMessage().contains("userId")));
    }
  }
}
