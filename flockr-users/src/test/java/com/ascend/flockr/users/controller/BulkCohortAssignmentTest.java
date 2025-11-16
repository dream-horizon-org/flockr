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
    String cohortName = "test-cohort";
    String csvContent = "550e8400-e29b-41d4-a716-446655440000";
    byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csvFile", Collections.singletonList(csvFilePart));
    formDataMap.put("cohortName", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(cohortName);
    when(csvFilePart.getBody(byte[].class, null)).thenReturn(csvBytes);

    BulkOperationResult operationResult =
        new BulkOperationResult(1, 1, 0, "Processed 1 users successfully");

    when(userCohortsService.assignUsersToCohort(eq(cohortName), any(InputPart.class)))
        .thenReturn(Single.just(operationResult));

    // Act
    CompletionStage<Response> responseStage = controller.bulkAssignUsers(multipartInput);
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
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("cohortName", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    try {
      controller.bulkAssignUsers(multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown for missing csvFile");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("Missing csvFile")
                  || e.getCause().getMessage().contains("INVALID_REQUEST_PARAMS")));
    }
  }

  @Test
  public void bulkAssignUsers_WithMissingCohortName_ThrowsException() {
    // Arrange
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csvFile", Collections.singletonList(csvFilePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    try {
      controller.bulkAssignUsers(multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown for missing cohortName");
    } catch (Exception e) {
      assertTrue(
          e.getCause() != null
              && (e.getCause().getMessage().contains("Missing cohortName")
                  || e.getCause().getMessage().contains("INVALID_REQUEST_PARAMS")));
    }
  }

  @Test
  public void bulkAssignUsers_WithServiceError_ReturnsServerError() throws Exception {
    // Arrange
    String cohortName = "test-cohort";
    String csvContent = "550e8400-e29b-41d4-a716-446655440000";
    byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csvFile", Collections.singletonList(csvFilePart));
    formDataMap.put("cohortName", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(cohortName);
    when(csvFilePart.getBody(byte[].class, null)).thenReturn(csvBytes);

    RuntimeException serviceError = new RuntimeException("Service error");
    when(userCohortsService.assignUsersToCohort(eq(cohortName), any(InputPart.class)))
        .thenReturn(Single.error(serviceError));

    // Act
    CompletionStage<Response> responseStage = controller.bulkAssignUsers(multipartInput);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof ResponseEntity.Failure);
  }

  @Test
  public void bulkAssignUsers_WithEmptyCohortName_ProcessesRequest() throws Exception {
    // Arrange
    String cohortName = "   "; // Whitespace only
    String csvContent = "550e8400-e29b-41d4-a716-446655440000";
    byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csvFile", Collections.singletonList(csvFilePart));
    formDataMap.put("cohortName", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(cohortName);
    when(csvFilePart.getBody(byte[].class, null)).thenReturn(csvBytes);

    BulkOperationResult operationResult = new BulkOperationResult(0, 0, 0, "No users processed");

    when(userCohortsService.assignUsersToCohort(anyString(), any(InputPart.class)))
        .thenReturn(Single.just(operationResult));

    // Act
    CompletionStage<Response> responseStage = controller.bulkAssignUsers(multipartInput);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void extractPart_WithValidInput_ReturnsTrimmedString() throws Exception {
    // Arrange
    String expectedValue = "  test-value  ";
    String trimmedValue = "test-value";

    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("testField", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(expectedValue);

    // Act - This tests the private extractPart method indirectly
    // We test it through bulkAssignUsers which uses it
    when(csvFilePart.getBody(byte[].class, null))
        .thenReturn("test".getBytes(StandardCharsets.UTF_8));
    formDataMap.put("csvFile", Collections.singletonList(csvFilePart));

    BulkOperationResult operationResult = new BulkOperationResult(0, 0, 0, "Test");

    when(userCohortsService.assignUsersToCohort(eq(trimmedValue), any(InputPart.class)))
        .thenReturn(Single.just(operationResult));

    CompletionStage<Response> responseStage = controller.bulkAssignUsers(multipartInput);
    Response response = responseStage.toCompletableFuture().get();

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(userCohortsService).assignUsersToCohort(eq(trimmedValue), any(InputPart.class));
  }

  @Test
  public void extractPart_WithIOException_ThrowsRuntimeException() throws Exception {
    // Arrange
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("testField", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenThrow(new IOException("IO error"));

    // Act & Assert
    try {
      // This will trigger extractPart which will throw IOException
      // and it should be wrapped in RuntimeException
      Map<String, List<InputPart>> formDataMap2 = new HashMap<>();
      formDataMap2.put("csvFile", Collections.singletonList(csvFilePart));
      formDataMap2.put("cohortName", Collections.singletonList(cohortNamePart));
      when(multipartInput.getFormDataMap()).thenReturn(formDataMap2);
      when(cohortNamePart.getBodyAsString()).thenThrow(new IOException("IO error"));

      controller.bulkAssignUsers(multipartInput).toCompletableFuture().get();
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
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("otherField", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    try {
      controller.bulkAssignUsers(multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void getPart_WithEmptyPartsList_ThrowsException() {
    // Arrange
    Map<String, List<InputPart>> formDataMap = new HashMap<>();
    formDataMap.put("csvFile", Collections.emptyList());

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    try {
      controller.bulkAssignUsers(multipartInput).toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }
}
