package io.ascend.flockr.users.controller;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.ascend.flockr.users.dto.ResponseEntity;
import io.ascend.flockr.users.service.UserCohortsService;
import io.reactivex.rxjava3.core.Single;
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
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    List<String> cohorts = Arrays.asList("cohort1", "cohort2", "cohort3");

    when(userCohortsService.getCohorts(eq(userIdHeader), eq(projectKey)))
        .thenReturn(Single.just(cohorts));

    // Act
    CompletionStage<ResponseEntity.Success<List<String>>> responseStage =
        controller.handle(userIdHeader, projectKey);
    ResponseEntity.Success<List<String>> response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(cohorts, response.data());
  }

  @Test
  public void handle_WithEmptyCohortsList_ReturnsEmptyList() throws Exception {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    List<String> emptyCohorts = Collections.emptyList();

    when(userCohortsService.getCohorts(eq(userIdHeader), eq(projectKey)))
        .thenReturn(Single.just(emptyCohorts));

    // Act
    CompletionStage<ResponseEntity.Success<List<String>>> responseStage =
        controller.handle(userIdHeader, projectKey);
    ResponseEntity.Success<List<String>> response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(emptyCohorts, response.data());
  }

  @Test
  public void handle_WithMissingUserIdHeader_PassesToService() throws Exception {
    // Arrange
    String userIdHeader = null;
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    List<String> emptyCohorts = Collections.emptyList();

    // Controller doesn't validate userId header, it passes it directly to the service
    when(userCohortsService.getCohorts(eq(userIdHeader), eq(projectKey)))
        .thenReturn(Single.just(emptyCohorts));

    // Act
    CompletionStage<ResponseEntity.Success<List<String>>> responseStage =
        controller.handle(userIdHeader, projectKey);
    ResponseEntity.Success<List<String>> response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(emptyCohorts, response.data());
  }

  @Test
  public void handle_WithMissingProjectKeyHeader_ThrowsException() {
    // Arrange
    String userIdHeader = "123";
    String projectKey = null;

    // Act & Assert - HeaderValidator throws synchronously
    try {
      controller.handle(userIdHeader, projectKey);
      fail("Expected exception to be thrown for missing x-project-key header");
    } catch (Exception e) {
      String message = e.getMessage();
      Throwable cause = e.getCause();
      assertTrue(
          (message != null
                  && (message.contains("MISSING_PROJECT_KEY_HEADER")
                      || message.contains("x-project-key")))
              || (cause != null
                  && (cause.getMessage().contains("MISSING_PROJECT_KEY_HEADER")
                      || cause.getMessage().contains("x-project-key"))));
    }
  }

  @Test
  public void handle_WithInvalidProjectKeyFormat_PassesValidation() throws Exception {
    // Arrange
    String userIdHeader = "123";
    String projectKey =
        "invalid-format"; // Missing underscore - format is not validated by controller
    List<String> cohorts = Arrays.asList("cohort1");

    when(userCohortsService.getCohorts(eq(userIdHeader), eq(projectKey)))
        .thenReturn(Single.just(cohorts));

    // Act & Assert - Controller doesn't validate projectKey format, only checks if null/empty
    CompletionStage<ResponseEntity.Success<List<String>>> responseStage =
        controller.handle(userIdHeader, projectKey);
    ResponseEntity.Success<List<String>> response = responseStage.toCompletableFuture().get();

    assertNotNull(response);
    assertEquals(cohorts, response.data());
  }

  @Test
  public void handle_WithInvalidTenantIdFormat_PassesValidation() throws Exception {
    // Arrange
    String userIdHeader = "123";
    String projectKey =
        "invalid-uuid_project-100"; // Invalid UUID format - not validated by controller
    List<String> cohorts = Arrays.asList("cohort1");

    when(userCohortsService.getCohorts(eq(userIdHeader), eq(projectKey)))
        .thenReturn(Single.just(cohorts));

    // Act & Assert - Controller doesn't validate tenantId format
    CompletionStage<ResponseEntity.Success<List<String>>> responseStage =
        controller.handle(userIdHeader, projectKey);
    ResponseEntity.Success<List<String>> response = responseStage.toCompletableFuture().get();

    assertNotNull(response);
    assertEquals(cohorts, response.data());
  }

  @Test
  public void handle_WithInvalidUserId_PassesToService() throws Exception {
    // Arrange
    String userIdHeader = "-1";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    List<String> emptyCohorts = Collections.emptyList();

    // Controller doesn't validate userId format, it passes it directly to the service
    when(userCohortsService.getCohorts(eq(userIdHeader), eq(projectKey)))
        .thenReturn(Single.just(emptyCohorts));

    // Act
    CompletionStage<ResponseEntity.Success<List<String>>> responseStage =
        controller.handle(userIdHeader, projectKey);
    ResponseEntity.Success<List<String>> response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(emptyCohorts, response.data());
  }

  @Test
  public void handle_WithZeroUserId_PassesToService() throws Exception {
    // Arrange
    String userIdHeader = "0";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    List<String> emptyCohorts = Collections.emptyList();

    // Controller doesn't validate userId format
    when(userCohortsService.getCohorts(eq(userIdHeader), eq(projectKey)))
        .thenReturn(Single.just(emptyCohorts));

    // Act
    CompletionStage<ResponseEntity.Success<List<String>>> responseStage =
        controller.handle(userIdHeader, projectKey);
    ResponseEntity.Success<List<String>> response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(emptyCohorts, response.data());
  }

  @Test
  public void handle_WithServiceError_PropagatesError() {
    // Arrange
    String userIdHeader = "123";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    RuntimeException serviceError = new RuntimeException("Service error");

    when(userCohortsService.getCohorts(eq(userIdHeader), eq(projectKey)))
        .thenReturn(Single.error(serviceError));

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey).toCompletableFuture().get();
      fail("Expected exception to be propagated");
    } catch (Exception e) {
      assertNotNull(e);
      // The error should be propagated from the service
    }
  }
}
