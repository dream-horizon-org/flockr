package com.ascend.flockr.users.controller;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ascend.flockr.users.dto.BulkOperationResult;
import com.ascend.flockr.users.dto.ResponseEntity;
import com.ascend.flockr.users.service.UserCohortsService;
import io.reactivex.rxjava3.core.Single;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletionStage;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link BulkCohortAssignment} controller.
 *
 * <p>Tests cover request validation, error handling, and response formatting for bulk cohort
 * assignment operations.
 *
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class BulkCohortAssignmentTest {

  @Mock private UserCohortsService userCohortsService;

  @Mock private MultipartFormDataInput multipartInput;

  @Mock private InputPart csvFilePart;

  @Mock private InputPart cohortNamePart;

  @InjectMocks private BulkCohortAssignment controller;

  @Before
  public void setUp() {
    // Setup is handled by MockitoJUnitRunner
  }

  @Test
  public void bulkAssignUsers_WithValidRequest_ReturnsSuccessResponse() throws Exception {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String cohortName = "test-cohort";
    String csvContent = "550e8400-e29b-41d4-a716-446655440000";

    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(cohortName);
    when(csvFilePart.getBody(java.io.InputStream.class, null))
        .thenReturn(new java.io.ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    BulkOperationResult operationResult =
        new BulkOperationResult(1, 1, 0, "Processed 1 users successfully");

    when(userCohortsService.assignUsersToCohort(eq(cohortName), eq(tenantId), eq(projectId), any(InputPart.class)))
        .thenReturn(Single.just(operationResult));

    // Act
    CompletionStage<Response> responseStage = controller.bulkAssignUsers(projectKey, multipartInput);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof ResponseEntity.Success);
  }

  @Test
  public void bulkAssignUsers_WithMissingCsvFile_ThrowsException() {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    try {
      controller.bulkAssignUsers(projectKey, multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown for missing csv_file");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("MISSING_CSV_FILE")
                  || e.getCause().getMessage().contains("csv_file")));
    }
  }

  @Test
  public void bulkAssignUsers_WithMissingCohortName_ThrowsException() {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    try {
      controller.bulkAssignUsers(projectKey, multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown for missing cohort_name");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("MISSING_COHORT_NAME")
                  || e.getCause().getMessage().contains("cohort_name")));
    }
  }

  @Test
  public void bulkAssignUsers_WithMissingProjectKeyHeader_ThrowsException() {
    // Arrange
    String projectKey = null;
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    try {
      controller.bulkAssignUsers(projectKey, multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown for missing x-project-key header");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("MISSING_PROJECT_KEY_HEADER")
                  || e.getCause().getMessage().contains("x-project-key")));
    }
  }

  @Test
  public void bulkAssignUsers_WithInvalidProjectKeyFormat_ThrowsException() {
    // Arrange
    String projectKey = "invalid-format"; // Missing underscore
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    try {
      controller.bulkAssignUsers(projectKey, multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown for invalid x-project-key format");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_PROJECT_KEY_FORMAT")
                  || e.getCause().getMessage().contains("x-project-key")));
    }
  }

  @Test
  public void bulkAssignUsers_WithInvalidTenantIdFormat_ThrowsException() {
    // Arrange
    String projectKey = "invalid-uuid_project-100"; // Invalid UUID format
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    try {
      controller.bulkAssignUsers(projectKey, multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown for invalid tenantId format");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("INVALID_TENANT_ID_FORMAT")
                  || e.getCause().getMessage().contains("UUID")));
    }
  }

  @Test
  public void bulkAssignUsers_WithServiceError_ReturnsServerError() throws Exception {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String cohortName = "test-cohort";
    String csvContent = "550e8400-e29b-41d4-a716-446655440000";

    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(cohortName);
    when(csvFilePart.getBody(java.io.InputStream.class, null))
        .thenReturn(new java.io.ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    RuntimeException serviceError = new RuntimeException("Service error");
    when(userCohortsService.assignUsersToCohort(eq(cohortName), eq(tenantId), eq(projectId), any(InputPart.class)))
        .thenReturn(Single.error(serviceError));

    // Act
    CompletionStage<Response> responseStage = controller.bulkAssignUsers(projectKey, multipartInput);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof ResponseEntity.Failure);
  }

  @Test
  public void bulkAssignUsers_WithEmptyCohortName_ThrowsException() {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    String cohortName = "   "; // Whitespace only
    String csvContent = "550e8400-e29b-41d4-a716-446655440000";

    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    try {
      when(cohortNamePart.getBodyAsString()).thenReturn(cohortName);
      when(csvFilePart.getBody(java.io.InputStream.class, null))
          .thenReturn(new java.io.ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));
    } catch (IOException e) {
      // Mock setup can throw, but we'll handle it in the test
    }

    // Act & Assert
    try {
      controller.bulkAssignUsers(projectKey, multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown for empty cohort_name");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("MISSING_COHORT_NAME")
                  || e.getCause().getMessage().contains("cohort_name")));
    }
  }

  @Test
  public void extractPart_WithValidInput_ReturnsTrimmedString() throws Exception {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    String tenantId = "550e8400-e29b-41d4-a716-446655440000";
    String projectId = "project-100";
    String expectedValue = "  test-value  ";
    String trimmedValue = "test-value";
    String csvContent = "550e8400-e29b-41d4-a716-446655440000";

    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(expectedValue);
    when(csvFilePart.getBody(java.io.InputStream.class, null))
        .thenReturn(new java.io.ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

    BulkOperationResult operationResult = new BulkOperationResult(0, 0, 0, "Test");

    when(userCohortsService.assignUsersToCohort(eq(trimmedValue), eq(tenantId), eq(projectId), any(InputPart.class)))
        .thenReturn(Single.just(operationResult));

    CompletionStage<Response> responseStage = controller.bulkAssignUsers(projectKey, multipartInput);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(userCohortsService).assignUsersToCohort(eq(trimmedValue), eq(tenantId), eq(projectId), any(InputPart.class));
  }

  @Test
  public void extractPart_WithIOException_ThrowsRuntimeException() {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    try {
      when(cohortNamePart.getBodyAsString()).thenThrow(new IOException("IO error"));
    } catch (Exception e) {
      // Mock setup can throw, but we'll handle it in the test
    }

    // Act & Assert
    try {
      controller.bulkAssignUsers(projectKey, multipartInput).toCompletableFuture().get();
      fail("Expected RuntimeException to be thrown");
    } catch (Exception e) {
      assertTrue(
          e.getCause() instanceof RuntimeException
              || e.getCause() instanceof IOException
              || e.getCause() == null);
    }
  }

  @Test
  public void getPart_WithNullParts_ThrowsException() {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("otherField", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    try {
      controller.bulkAssignUsers(projectKey, multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void getPart_WithEmptyPartsList_ThrowsException() {
    // Arrange
    String projectKey = "550e8400-e29b-41d4-a716-446655440000_project-100";
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csv_file", Collections.emptyList());

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    try {
      controller.bulkAssignUsers(projectKey, multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }
}
