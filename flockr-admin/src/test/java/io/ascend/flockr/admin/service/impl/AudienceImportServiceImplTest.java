package io.ascend.flockr.admin.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dream11.rest.exception.RestException;
import io.ascend.flockr.admin.client.sink.SinkPusherRegistry;
import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.exception.ResourceNotFoundException;
import io.ascend.flockr.admin.io.request.CsvImportForm;
import io.ascend.flockr.admin.io.response.AudienceImportResponse;
import io.ascend.flockr.admin.repository.AudienceRepository;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * Unit tests for {@link AudienceImportServiceImpl}.
 *
 * <p>These tests verify the CSV import service logic including audience validation, CSV processing,
 * and sink pushing.
 *
 * <p>Note: The service uses subscribeOn(Schedulers.io()) for CSV processing, so tests must use
 * awaitDone() to wait for async completion.
 */
public class AudienceImportServiceImplTest {

  private static final String PROJECT_ID = "test-project";
  private static final Long AUDIENCE_ID = 42L;
  private static final String ACTOR_EMAIL = "actor@example.com";
  private static final int TIMEOUT_SECONDS = 5;

  private AudienceImportServiceImpl buildService(
      AudienceRepository audienceRepository,
      DataConnectorRepository dataConnectorRepository,
      SinkPusherRegistry sinkPusherRegistry) {
    return new AudienceImportServiceImpl(
        audienceRepository, dataConnectorRepository, sinkPusherRegistry);
  }

  private static AudienceMeta buildStaticAudience() {
    return AudienceMeta.builder()
        .xProjectId(PROJECT_ID)
        .audienceId(AUDIENCE_ID)
        .name("Static Audience")
        .type("STATIC")
        .sinks(List.of(1L, 2L))
        .build();
  }

  private static AudienceMeta buildConditionalAudience() {
    return AudienceMeta.builder()
        .xProjectId(PROJECT_ID)
        .audienceId(AUDIENCE_ID)
        .name("Conditional Audience")
        .type("CONDITIONAL")
        .sinks(List.of(1L))
        .build();
  }

  private static CsvImportForm buildCsvForm(String content, String fileName, String action) {
    CsvImportForm form = new CsvImportForm();
    form.setFileName(fileName);
    form.setAction(action);
    form.setFile(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    return form;
  }

  // ========================================
  // createImport Tests - Success Scenarios
  // ========================================

  @Test
  public void createImport_success_returnsCompletedResponse() throws InterruptedException {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta staticAudience = buildStaticAudience();
    List<DataSinkDetails> sinks =
        List.of(
            DataSinkDetails.builder().id(1L).name("Sink 1").type("KAFKA").build(),
            DataSinkDetails.builder().id(2L).name("Sink 2").type("S3").build());

    CsvImportForm form = buildCsvForm("user_id\n123\n456\n789", "users.csv", "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));
    when(dataConnectorRepository.getDataSinksByIds(eq(List.of(1L, 2L))))
        .thenReturn(Single.just(sinks));
    when(sinkPusherRegistry.pushBatchToAll(anyList(), anyList(), any(AudienceMeta.class)))
        .thenReturn(Completable.complete());

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert - await completion since CSV processing is async
    to.awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    to.assertComplete();
    to.assertValue(
        response -> {
          assertEquals(AUDIENCE_ID, response.getAudienceId());
          assertEquals("users.csv", response.getFileName());
          assertEquals(Long.valueOf(3L), response.getRecordCount());
          assertEquals("COMPLETED", response.getStatus());
          return true;
        });
  }

  @Test
  public void createImport_withRemoveAction_success() throws InterruptedException {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta staticAudience = buildStaticAudience();
    CsvImportForm form = buildCsvForm("user_id\n123", "remove.csv", "remove");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));
    when(dataConnectorRepository.getDataSinksByIds(anyList()))
        .thenReturn(Single.just(Collections.emptyList()));
    when(sinkPusherRegistry.pushBatchToAll(anyList(), anyList(), any(AudienceMeta.class)))
        .thenReturn(Completable.complete());

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert
    to.awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    to.assertComplete();
    to.assertValue(response -> "COMPLETED".equals(response.getStatus()));
  }

  @Test
  public void createImport_withNullFileName_usesDefaultFileName() throws InterruptedException {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta staticAudience = buildStaticAudience();
    CsvImportForm form = buildCsvForm("user_id\n123", null, "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));
    when(dataConnectorRepository.getDataSinksByIds(anyList()))
        .thenReturn(Single.just(Collections.emptyList()));
    when(sinkPusherRegistry.pushBatchToAll(anyList(), anyList(), any(AudienceMeta.class)))
        .thenReturn(Completable.complete());

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert
    to.awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    to.assertComplete();
    to.assertValue(response -> "upload.csv".equals(response.getFileName()));
  }

  @Test
  public void createImport_withNoSinks_success() throws InterruptedException {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta staticAudience =
        AudienceMeta.builder()
            .xProjectId(PROJECT_ID)
            .audienceId(AUDIENCE_ID)
            .name("Static Audience")
            .type("STATIC")
            .sinks(Collections.emptyList())
            .build();

    CsvImportForm form = buildCsvForm("user_id\n123", "users.csv", "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));
    when(dataConnectorRepository.getDataSinksByIds(eq(Collections.emptyList())))
        .thenReturn(Single.just(Collections.emptyList()));

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert
    to.awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    to.assertComplete();
    to.assertValue(response -> "COMPLETED".equals(response.getStatus()));
  }

  // ========================================
  // createImport Tests - Error Scenarios
  // ========================================

  @Test
  public void createImport_audienceNotFound_throwsResourceNotFoundException() {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    CsvImportForm form = buildCsvForm("user_id\n123", "users.csv", "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.error(new NoSuchElementException("Not found")));

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert - errors happen synchronously before async processing
    to.assertError(ResourceNotFoundException.class);
  }

  @Test
  public void createImport_conditionalAudience_throwsException() {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta conditionalAudience = buildConditionalAudience();
    CsvImportForm form = buildCsvForm("user_id\n123", "users.csv", "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(conditionalAudience));

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert - error happens synchronously before async processing
    to.assertError(RestException.class);
  }

  @Test
  public void createImport_repositoryError_propagatesException() {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    CsvImportForm form = buildCsvForm("user_id\n123", "users.csv", "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.error(new RuntimeException("Database error")));

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert
    to.assertError(RuntimeException.class);
  }

  @Test
  public void createImport_sinkFetchError_propagatesException() {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta staticAudience = buildStaticAudience();
    CsvImportForm form = buildCsvForm("user_id\n123", "users.csv", "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));
    when(dataConnectorRepository.getDataSinksByIds(anyList()))
        .thenReturn(Single.error(new RuntimeException("Failed to fetch sinks")));

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert
    to.assertError(RuntimeException.class);
  }

  // ========================================
  // CSV Parsing Tests
  // ========================================

  @Test
  public void createImport_withUserIdColumn_success() throws InterruptedException {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta staticAudience = buildStaticAudience();
    // CSV with user_id in second column
    CsvImportForm form =
        buildCsvForm("name,user_id,email\nJohn,123,john@test.com", "users.csv", "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));
    when(dataConnectorRepository.getDataSinksByIds(anyList()))
        .thenReturn(Single.just(Collections.emptyList()));

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert
    to.awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    to.assertComplete();
    to.assertValue(response -> response.getRecordCount() == 1L);
  }

  @Test
  public void createImport_withUserIdAlternateHeader_success() throws InterruptedException {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta staticAudience = buildStaticAudience();
    // CSV with "userid" header (no underscore)
    CsvImportForm form = buildCsvForm("userid\n123\n456", "users.csv", "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));
    when(dataConnectorRepository.getDataSinksByIds(anyList()))
        .thenReturn(Single.just(Collections.emptyList()));

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert
    to.awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    to.assertComplete();
    to.assertValue(response -> response.getRecordCount() == 2L);
  }

  @Test
  public void createImport_withIdHeader_success() throws InterruptedException {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta staticAudience = buildStaticAudience();
    // CSV with just "id" header
    CsvImportForm form = buildCsvForm("id\n123", "users.csv", "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));
    when(dataConnectorRepository.getDataSinksByIds(anyList()))
        .thenReturn(Single.just(Collections.emptyList()));

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert
    to.awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    to.assertComplete();
    to.assertValue(response -> response.getRecordCount() == 1L);
  }

  @Test
  public void createImport_skipsEmptyRows_success() throws InterruptedException {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta staticAudience = buildStaticAudience();
    // CSV with empty user_id rows
    CsvImportForm form = buildCsvForm("user_id\n123\n\n456\n  \n789", "users.csv", "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));
    when(dataConnectorRepository.getDataSinksByIds(anyList()))
        .thenReturn(Single.just(Collections.emptyList()));

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert
    to.awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    to.assertComplete();
    // Should only count rows with valid user_id (123, 456, 789)
    to.assertValue(response -> response.getRecordCount() == 3L);
  }

  @Test
  public void createImport_withQuotedValues_success() throws InterruptedException {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta staticAudience = buildStaticAudience();
    // CSV with quoted user_id values
    CsvImportForm form = buildCsvForm("user_id\n\"123\"\n\"456\"", "users.csv", "add");

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));
    when(dataConnectorRepository.getDataSinksByIds(anyList()))
        .thenReturn(Single.just(Collections.emptyList()));

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert
    to.awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    to.assertComplete();
    to.assertValue(response -> response.getRecordCount() == 2L);
  }

  // ========================================
  // Action Tests
  // ========================================

  @Test
  public void createImport_withNullAction_defaultsToAdd() throws InterruptedException {
    // Arrange
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    SinkPusherRegistry sinkPusherRegistry = mock(SinkPusherRegistry.class);
    AudienceImportServiceImpl service =
        buildService(audienceRepository, dataConnectorRepository, sinkPusherRegistry);

    AudienceMeta staticAudience = buildStaticAudience();
    CsvImportForm form = buildCsvForm("user_id\n123", "users.csv", null);

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));
    when(dataConnectorRepository.getDataSinksByIds(anyList()))
        .thenReturn(Single.just(Collections.emptyList()));

    // Act
    TestObserver<AudienceImportResponse> to =
        service.createImport(PROJECT_ID, AUDIENCE_ID, form, ACTOR_EMAIL).test();

    // Assert - should succeed with default "add" action
    to.awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    to.assertComplete();
  }
}
