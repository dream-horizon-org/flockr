package io.ascend.flockr.admin.rest;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.ascend.flockr.admin.io.ResponseEntity;
import io.ascend.flockr.admin.io.request.CsvImportForm;
import io.ascend.flockr.admin.io.response.AudienceImportResponse;
import io.ascend.flockr.admin.service.AudienceImportService;
import io.reactivex.rxjava3.core.Single;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

/**
 * Unit tests for {@link AudienceImportController}.
 *
 * <p>These tests verify that the controller correctly delegates to the service layer and handles
 * responses appropriately for CSV imports.
 */
public class AudienceImportControllerTest {

  private static final String PROJECT_ID = "test-project";
  private static final String ACTOR_EMAIL = "actor@example.com";
  private static final Long AUDIENCE_ID = 42L;

  // ========================================
  // createImport Tests
  // ========================================

  @Test
  public void createImport_success_returnsImportResponse() {
    // Arrange
    AudienceImportService service = mock(AudienceImportService.class);
    AudienceImportController controller = new AudienceImportController(service);

    CsvImportForm form = new CsvImportForm();
    form.setFileName("users.csv");
    form.setAction("add");
    form.setFile(new ByteArrayInputStream("user_id\n123\n456".getBytes(StandardCharsets.UTF_8)));

    AudienceImportResponse expectedResponse =
        AudienceImportResponse.builder()
            .audienceId(AUDIENCE_ID)
            .fileName("users.csv")
            .recordCount(2L)
            .status("COMPLETED")
            .build();

    when(service.createImport(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(form), eq(ACTOR_EMAIL)))
        .thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<AudienceImportResponse>> result =
        controller.createImport(PROJECT_ID, ACTOR_EMAIL, AUDIENCE_ID, form);
    ResponseEntity.Success<AudienceImportResponse> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(AUDIENCE_ID, response.data().getAudienceId());
    assertEquals("users.csv", response.data().getFileName());
    assertEquals(Long.valueOf(2L), response.data().getRecordCount());
    assertEquals("COMPLETED", response.data().getStatus());
    verify(service).createImport(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(form), eq(ACTOR_EMAIL));
  }

  @Test
  public void createImport_withDefaultActor_usesSystem() {
    // Arrange
    AudienceImportService service = mock(AudienceImportService.class);
    AudienceImportController controller = new AudienceImportController(service);

    CsvImportForm form = new CsvImportForm();
    form.setFileName("users.csv");
    form.setFile(new ByteArrayInputStream("user_id\n123".getBytes(StandardCharsets.UTF_8)));

    AudienceImportResponse expectedResponse =
        AudienceImportResponse.builder()
            .audienceId(AUDIENCE_ID)
            .fileName("users.csv")
            .recordCount(1L)
            .status("COMPLETED")
            .build();

    when(service.createImport(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(form), eq("system")))
        .thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<AudienceImportResponse>> result =
        controller.createImport(PROJECT_ID, "system", AUDIENCE_ID, form);
    ResponseEntity.Success<AudienceImportResponse> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertEquals(Long.valueOf(1L), response.data().getRecordCount());
    verify(service).createImport(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(form), eq("system"));
  }

  @Test
  public void createImport_withRemoveAction_success() {
    // Arrange
    AudienceImportService service = mock(AudienceImportService.class);
    AudienceImportController controller = new AudienceImportController(service);

    CsvImportForm form = new CsvImportForm();
    form.setFileName("users_to_remove.csv");
    form.setAction("remove");
    form.setFile(new ByteArrayInputStream("user_id\n789".getBytes(StandardCharsets.UTF_8)));

    AudienceImportResponse expectedResponse =
        AudienceImportResponse.builder()
            .audienceId(AUDIENCE_ID)
            .fileName("users_to_remove.csv")
            .recordCount(1L)
            .status("COMPLETED")
            .build();

    when(service.createImport(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(form), eq(ACTOR_EMAIL)))
        .thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<AudienceImportResponse>> result =
        controller.createImport(PROJECT_ID, ACTOR_EMAIL, AUDIENCE_ID, form);
    ResponseEntity.Success<AudienceImportResponse> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertEquals("COMPLETED", response.data().getStatus());
  }

  @Test
  public void createImport_audienceNotFound_propagatesException() {
    // Arrange
    AudienceImportService service = mock(AudienceImportService.class);
    AudienceImportController controller = new AudienceImportController(service);

    CsvImportForm form = new CsvImportForm();
    form.setFileName("users.csv");
    form.setFile(new ByteArrayInputStream("user_id\n123".getBytes(StandardCharsets.UTF_8)));

    RuntimeException expectedException = new RuntimeException("Audience not found: 999");
    when(service.createImport(eq(PROJECT_ID), eq(999L), eq(form), eq(ACTOR_EMAIL)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<AudienceImportResponse>> result =
        controller.createImport(PROJECT_ID, ACTOR_EMAIL, 999L, form);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("not found"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  @Test
  public void createImport_conditionalAudience_propagatesException() {
    // Arrange
    AudienceImportService service = mock(AudienceImportService.class);
    AudienceImportController controller = new AudienceImportController(service);

    CsvImportForm form = new CsvImportForm();
    form.setFileName("users.csv");
    form.setFile(new ByteArrayInputStream("user_id\n123".getBytes(StandardCharsets.UTF_8)));

    RuntimeException expectedException =
        new RuntimeException("CSV import not allowed for CONDITIONAL audiences");
    when(service.createImport(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(form), eq(ACTOR_EMAIL)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<AudienceImportResponse>> result =
        controller.createImport(PROJECT_ID, ACTOR_EMAIL, AUDIENCE_ID, form);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("CONDITIONAL"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  @Test
  public void createImport_invalidCsv_propagatesException() {
    // Arrange
    AudienceImportService service = mock(AudienceImportService.class);
    AudienceImportController controller = new AudienceImportController(service);

    CsvImportForm form = new CsvImportForm();
    form.setFileName("invalid.csv");
    form.setFile(new ByteArrayInputStream("invalid data".getBytes(StandardCharsets.UTF_8)));

    RuntimeException expectedException = new RuntimeException("Invalid CSV format");
    when(service.createImport(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(form), eq(ACTOR_EMAIL)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<AudienceImportResponse>> result =
        controller.createImport(PROJECT_ID, ACTOR_EMAIL, AUDIENCE_ID, form);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Invalid"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  @Test
  public void createImport_serviceThrowsException_propagatesException() {
    // Arrange
    AudienceImportService service = mock(AudienceImportService.class);
    AudienceImportController controller = new AudienceImportController(service);

    CsvImportForm form = new CsvImportForm();
    form.setFileName("users.csv");
    form.setFile(new ByteArrayInputStream("user_id\n123".getBytes(StandardCharsets.UTF_8)));

    RuntimeException expectedException = new RuntimeException("Sink push failed");
    when(service.createImport(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(form), eq(ACTOR_EMAIL)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<AudienceImportResponse>> result =
        controller.createImport(PROJECT_ID, ACTOR_EMAIL, AUDIENCE_ID, form);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Sink push failed"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  @Test
  public void createImport_largeRecordCount_success() {
    // Arrange
    AudienceImportService service = mock(AudienceImportService.class);
    AudienceImportController controller = new AudienceImportController(service);

    CsvImportForm form = new CsvImportForm();
    form.setFileName("large_users.csv");
    form.setFile(new ByteArrayInputStream("user_id\n123".getBytes(StandardCharsets.UTF_8)));

    AudienceImportResponse expectedResponse =
        AudienceImportResponse.builder()
            .audienceId(AUDIENCE_ID)
            .fileName("large_users.csv")
            .recordCount(100000L)
            .status("COMPLETED")
            .build();

    when(service.createImport(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(form), eq(ACTOR_EMAIL)))
        .thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<AudienceImportResponse>> result =
        controller.createImport(PROJECT_ID, ACTOR_EMAIL, AUDIENCE_ID, form);
    ResponseEntity.Success<AudienceImportResponse> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertEquals(Long.valueOf(100000L), response.data().getRecordCount());
  }
}
