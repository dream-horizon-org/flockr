package io.ascend.flockr.users.controller;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dream11.rest.exception.RestException;
import io.ascend.flockr.users.dto.BulkOperationResult;
import io.ascend.flockr.users.dto.ResponseEntity;
import io.ascend.flockr.users.service.UserCohortsService;
import io.reactivex.rxjava3.core.Single;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
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
 * @author Sudhanshu Rai
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class BulkCohortAssignmentTest {

  private static final String VALID_PROJECT_KEY =
      "550e8400-e29b-41d4-a716-446655440000_project-100";
  private static final String VALID_COHORT_NAME = "test-cohort";

  @Mock private UserCohortsService userCohortsService;
  @Mock private MultipartFormDataInput multipartInput;
  @Mock private InputPart csvFilePart;
  @Mock private InputPart cohortNamePart;

  @InjectMocks private BulkCohortAssignment controller;

  private Map<String, List<InputPart>> formDataMap;

  @Before
  public void setUp() {
    formDataMap = new HashMap<>();
  }

  // ============================================================================
  // Success Cases
  // ============================================================================

  @Test
  public void bulkAssignUsers_WithValidRequest_ReturnsSuccessResponse() throws Exception {
    // Arrange
    setupValidFormData();
    BulkOperationResult expectedResult =
        new BulkOperationResult(3, 3, 0, "Processed 3 users successfully");

    when(userCohortsService.assignUsersToCohort(
            eq(VALID_COHORT_NAME), eq(VALID_PROJECT_KEY), any(InputPart.class)))
        .thenReturn(Single.just(expectedResult));

    // Act
    CompletionStage<ResponseEntity.Success<BulkOperationResult>> responseStage =
        controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput);
    ResponseEntity.Success<BulkOperationResult> response =
        responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(expectedResult, response.data());
    assertEquals(3, response.data().getTotalProcessed());
    assertEquals(3, response.data().getSuccessCount());
    assertEquals(0, response.data().getFailedCount());
  }

  @Test
  public void bulkAssignUsers_WithPartialFailures_ReturnsPartialSuccessResult() throws Exception {
    // Arrange
    setupValidFormData();
    BulkOperationResult expectedResult =
        new BulkOperationResult(100, 95, 5, "Bulk assignment completed: 95 succeeded, 5 failed");

    when(userCohortsService.assignUsersToCohort(
            eq(VALID_COHORT_NAME), eq(VALID_PROJECT_KEY), any(InputPart.class)))
        .thenReturn(Single.just(expectedResult));

    // Act
    ResponseEntity.Success<BulkOperationResult> response =
        controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput).toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertEquals(100, response.data().getTotalProcessed());
    assertEquals(95, response.data().getSuccessCount());
    assertEquals(5, response.data().getFailedCount());
  }

  @Test
  public void bulkAssignUsers_WithWhitespacePaddedCohortName_TrimsAndProcesses() throws Exception {
    // Arrange
    String paddedCohortName = "  trimmed-cohort  ";
    String expectedTrimmedName = "trimmed-cohort";

    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(paddedCohortName);

    BulkOperationResult expectedResult = new BulkOperationResult(1, 1, 0, "Success");
    when(userCohortsService.assignUsersToCohort(
            eq(expectedTrimmedName), eq(VALID_PROJECT_KEY), any(InputPart.class)))
        .thenReturn(Single.just(expectedResult));

    // Act
    ResponseEntity.Success<BulkOperationResult> response =
        controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput).toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    verify(userCohortsService)
        .assignUsersToCohort(eq(expectedTrimmedName), eq(VALID_PROJECT_KEY), any(InputPart.class));
  }

  // ============================================================================
  // Header Validation Cases
  // ============================================================================

  @Test
  public void bulkAssignUsers_WithNullProjectKey_ThrowsRestException() {
    // Act & Assert - Header validation happens before form data validation
    RestException exception =
        assertThrows(RestException.class, () -> controller.bulkAssignUsers(null, multipartInput));

    assertTrue(
        exception.getMessage().contains("x-project-key")
            || exception.getErrorCode().contains("MISSING_PROJECT_KEY"));
  }

  @Test
  public void bulkAssignUsers_WithEmptyProjectKey_ThrowsRestException() {
    // Act & Assert - Header validation happens before form data validation
    RestException exception =
        assertThrows(RestException.class, () -> controller.bulkAssignUsers("", multipartInput));

    assertTrue(
        exception.getMessage().contains("x-project-key")
            || exception.getErrorCode().contains("MISSING_PROJECT_KEY"));
  }

  @Test
  public void bulkAssignUsers_WithWhitespaceOnlyProjectKey_ThrowsRestException() {
    // Act & Assert - Header validation happens before form data validation
    RestException exception =
        assertThrows(RestException.class, () -> controller.bulkAssignUsers("   ", multipartInput));

    assertTrue(
        exception.getMessage().contains("x-project-key")
            || exception.getErrorCode().contains("MISSING_PROJECT_KEY"));
  }

  // ============================================================================
  // CSV File Validation Cases
  // ============================================================================

  @Test
  public void bulkAssignUsers_WithMissingCsvFile_ThrowsRestException() throws Exception {
    // Arrange
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));
    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(VALID_COHORT_NAME);

    // Act & Assert
    RestException exception =
        assertThrows(
            RestException.class,
            () -> controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput));

    assertTrue(
        exception.getMessage().contains("csv_file")
            || exception.getErrorCode().contains("MISSING_CSV_FILE"));
  }

  @Test
  public void bulkAssignUsers_WithNullCsvFileParts_ThrowsRestException() throws Exception {
    // Arrange
    formDataMap.put("csv_file", null);
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));
    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(VALID_COHORT_NAME);

    // Act & Assert
    RestException exception =
        assertThrows(
            RestException.class,
            () -> controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput));

    assertTrue(
        exception.getMessage().contains("csv_file")
            || exception.getErrorCode().contains("MISSING_CSV_FILE"));
  }

  @Test
  public void bulkAssignUsers_WithEmptyCsvFilePartsList_ThrowsRestException() throws Exception {
    // Arrange
    formDataMap.put("csv_file", Collections.emptyList());
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));
    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(VALID_COHORT_NAME);

    // Act & Assert
    RestException exception =
        assertThrows(
            RestException.class,
            () -> controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput));

    assertTrue(
        exception.getMessage().contains("csv_file")
            || exception.getErrorCode().contains("MISSING_CSV_FILE"));
  }

  // ============================================================================
  // Cohort Name Validation Cases
  // ============================================================================

  @Test
  public void bulkAssignUsers_WithMissingCohortName_ThrowsRestException() {
    // Arrange
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    RestException exception =
        assertThrows(
            RestException.class,
            () -> controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput));

    assertTrue(
        exception.getMessage().contains("cohort_name")
            || exception.getErrorCode().contains("MISSING_COHORT_NAME"));
  }

  @Test
  public void bulkAssignUsers_WithEmptyCohortName_ThrowsRestException() throws Exception {
    // Arrange
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn("");

    // Act & Assert
    RestException exception =
        assertThrows(
            RestException.class,
            () -> controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput));

    assertTrue(
        exception.getMessage().contains("cohort_name")
            || exception.getErrorCode().contains("MISSING_COHORT_NAME"));
  }

  @Test
  public void bulkAssignUsers_WithWhitespaceOnlyCohortName_ThrowsRestException() throws Exception {
    // Arrange
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn("   ");

    // Act & Assert
    RestException exception =
        assertThrows(
            RestException.class,
            () -> controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput));

    assertTrue(
        exception.getMessage().contains("cohort_name")
            || exception.getErrorCode().contains("MISSING_COHORT_NAME"));
  }

  @Test
  public void bulkAssignUsers_WithNullCohortNameFromBody_ThrowsRestException() throws Exception {
    // Arrange
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenReturn(null);

    // Act & Assert
    RestException exception =
        assertThrows(
            RestException.class,
            () -> controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput));

    assertTrue(
        exception.getMessage().contains("cohort_name")
            || exception.getErrorCode().contains("MISSING_COHORT_NAME"));
  }

  // ============================================================================
  // IO Error Cases
  // ============================================================================

  @Test
  public void bulkAssignUsers_WithIOExceptionReadingCohortName_ThrowsRuntimeException()
      throws Exception {
    // Arrange
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    when(cohortNamePart.getBodyAsString()).thenThrow(new IOException("Failed to read form data"));

    // Act & Assert
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput));

    assertNotNull(exception);
  }

  // ============================================================================
  // Service Error Cases
  // ============================================================================

  @Test
  public void bulkAssignUsers_WithServiceError_PropagatesException() throws Exception {
    // Arrange
    setupValidFormData();
    RuntimeException serviceError = new RuntimeException("Aerospike connection failed");

    when(userCohortsService.assignUsersToCohort(
            eq(VALID_COHORT_NAME), eq(VALID_PROJECT_KEY), any(InputPart.class)))
        .thenReturn(Single.error(serviceError));

    // Act & Assert
    ExecutionException exception =
        assertThrows(
            ExecutionException.class,
            () ->
                controller
                    .bulkAssignUsers(VALID_PROJECT_KEY, multipartInput)
                    .toCompletableFuture()
                    .get());

    assertNotNull(exception.getCause());
    // Error is mapped to RestException by ErrorHandler
    assertTrue(exception.getCause() instanceof RestException);
  }

  @Test
  public void bulkAssignUsers_WithIllegalArgumentFromService_ReturnsRestException()
      throws Exception {
    // Arrange
    setupValidFormData();
    IllegalArgumentException serviceError =
        new IllegalArgumentException("Invalid cohort name format");

    when(userCohortsService.assignUsersToCohort(
            eq(VALID_COHORT_NAME), eq(VALID_PROJECT_KEY), any(InputPart.class)))
        .thenReturn(Single.error(serviceError));

    // Act & Assert
    ExecutionException exception =
        assertThrows(
            ExecutionException.class,
            () ->
                controller
                    .bulkAssignUsers(VALID_PROJECT_KEY, multipartInput)
                    .toCompletableFuture()
                    .get());

    assertNotNull(exception.getCause());
    assertTrue(exception.getCause() instanceof RestException);
  }

  // ============================================================================
  // Edge Cases
  // ============================================================================

  @Test
  public void bulkAssignUsers_WithEmptyFormData_ThrowsRestException() {
    // Arrange
    when(multipartInput.getFormDataMap()).thenReturn(Collections.emptyMap());

    // Act & Assert
    RestException exception =
        assertThrows(
            RestException.class,
            () -> controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput));

    assertNotNull(exception);
  }

  @Test
  public void bulkAssignUsers_WithUnexpectedFormFields_ThrowsRestException() {
    // Arrange
    formDataMap.put("unexpected_field", Collections.singletonList(cohortNamePart));
    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act & Assert
    RestException exception =
        assertThrows(
            RestException.class,
            () -> controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput));

    assertNotNull(exception);
  }

  // ============================================================================
  // Service Interaction Verification
  // ============================================================================

  @Test
  public void bulkAssignUsers_VerifiesServiceCalledWithCorrectParameters() throws Exception {
    // Arrange
    setupValidFormData();
    BulkOperationResult expectedResult = new BulkOperationResult(1, 1, 0, "Success");

    when(userCohortsService.assignUsersToCohort(
            eq(VALID_COHORT_NAME), eq(VALID_PROJECT_KEY), any(InputPart.class)))
        .thenReturn(Single.just(expectedResult));

    // Act
    controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput).toCompletableFuture().get();

    // Assert
    verify(userCohortsService, times(1))
        .assignUsersToCohort(eq(VALID_COHORT_NAME), eq(VALID_PROJECT_KEY), eq(csvFilePart));
  }

  @Test
  public void bulkAssignUsers_ServiceNotCalledOnValidationFailure() {
    // Arrange
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));
    // Missing csv_file
    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);

    // Act
    try {
      controller.bulkAssignUsers(VALID_PROJECT_KEY, multipartInput);
    } catch (Exception e) {
      // Expected
    }

    // Assert
    verify(userCohortsService, never())
        .assignUsersToCohort(anyString(), anyString(), any(InputPart.class));
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  private void setupValidFormData() {
    formDataMap.put("csv_file", Collections.singletonList(csvFilePart));
    formDataMap.put("cohort_name", Collections.singletonList(cohortNamePart));

    when(multipartInput.getFormDataMap()).thenReturn(formDataMap);
    try {
      when(cohortNamePart.getBodyAsString()).thenReturn(VALID_COHORT_NAME);
      // Note: csvFilePart.getBody() is NOT stubbed here because the controller
      // passes the InputPart directly to the service without reading its body
    } catch (IOException e) {
      throw new RuntimeException("Failed to setup mocks", e);
    }
  }
}
