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
 * @author Sudhanshu Rai
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
  public void handle_WithMissingUserIdHeader_ThrowsException() {
    // Arrange
    String userIdHeader = null;
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey).toCompletableFuture().get();
      fail("Expected exception to be thrown for missing userId header");
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

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey).toCompletableFuture().get();
      fail("Expected exception to be thrown for missing x-project-key header");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("MISSING_PROJECT_KEY_HEADER")
                  || e.getCause().getMessage().contains("x-project-key")));
    }
  }

  @Test
  public void handle_WithNonNumericUserId_ThrowsNumberFormatException() {
    // Arrange
    String userIdHeader = "abc";
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";

    // Act & Assert
    try {
      controller.handle(userIdHeader, projectKey).toCompletableFuture().get();
      fail("Expected exception to be thrown for non-numeric userId");
    } catch (Exception e) {
      // NumberFormatException is thrown when parsing non-numeric userId
      assertNotNull(e);
      assertTrue(e.getCause() instanceof NumberFormatException);
    }
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
