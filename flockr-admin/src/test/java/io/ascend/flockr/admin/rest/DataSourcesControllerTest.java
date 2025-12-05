package io.ascend.flockr.admin.rest;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ascend.flockr.admin.domain.dataconnectors.DataConnectorType;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.io.ResponseEntity;
import io.ascend.flockr.admin.io.request.OnboardConnectorTypeRequest;
import io.ascend.flockr.admin.io.request.OnboardDataSinkRequest;
import io.ascend.flockr.admin.io.request.OnboardDataSourceRequest;
import io.ascend.flockr.admin.io.response.PaginatedResponse;
import io.ascend.flockr.admin.service.DataConnectorService;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

/**
 * Unit tests for {@link DataSourcesController}.
 *
 * <p>These tests verify that the controller correctly delegates to the service layer and handles
 * responses appropriately.
 */
public class DataSourcesControllerTest {

  private static final String USER_EMAIL = "user@example.com";
  private static final Long TYPE_ID = 1L;
  private static final Long SOURCE_ID = 100L;
  private static final Long SINK_ID = 200L;

  private DataSourcesController buildController(DataConnectorService service) {
    return new DataSourcesController(service, new ObjectMapper());
  }

  // ========================================
  // onboardConnectorType Tests
  // ========================================

  @Test
  public void onboardConnectorType_success_returnsConnectorType() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    OnboardConnectorTypeRequest request = new OnboardConnectorTypeRequest();
    request.setKind("SOURCE");
    request.setType("POSTGRES");
    request.setDisplayName("PostgreSQL");
    request.setConfigSchema(new JsonObject().put("type", "object"));

    DataConnectorType expectedType =
        DataConnectorType.builder()
            .id(TYPE_ID)
            .kind("SOURCE")
            .type("POSTGRES")
            .displayName("PostgreSQL")
            .active(true)
            .build();

    when(service.onboardConnectorType(eq(request), eq(USER_EMAIL)))
        .thenReturn(Single.just(expectedType));

    // Act
    CompletionStage<ResponseEntity.Success<DataConnectorType>> result =
        controller.onboardConnectorType(request, USER_EMAIL);
    ResponseEntity.Success<DataConnectorType> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(TYPE_ID, response.data().getId());
    assertEquals("POSTGRES", response.data().getType());
    assertEquals("PostgreSQL", response.data().getDisplayName());
    verify(service).onboardConnectorType(eq(request), eq(USER_EMAIL));
  }

  @Test
  public void onboardConnectorType_serviceThrowsException_propagatesException() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    OnboardConnectorTypeRequest request = new OnboardConnectorTypeRequest();
    request.setKind("INVALID");
    request.setType("POSTGRES");
    request.setDisplayName("PostgreSQL");
    request.setConfigSchema(new JsonObject());

    RuntimeException expectedException =
        new RuntimeException("Kind must be either 'SOURCE' or 'SINK'");
    when(service.onboardConnectorType(eq(request), eq(USER_EMAIL)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<DataConnectorType>> result =
        controller.onboardConnectorType(request, USER_EMAIL);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(
          e.getCause().getMessage().contains("SOURCE")
              || e.getCause().getMessage().contains("SINK"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // listTypes Tests
  // ========================================

  @Test
  public void listTypes_success_returnsTypesList() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    List<DataConnectorType> expectedTypes =
        List.of(
            DataConnectorType.builder().id(1L).kind("SOURCE").type("POSTGRES").build(),
            DataConnectorType.builder().id(2L).kind("SOURCE").type("KAFKA").build());

    when(service.listTypes(eq("SOURCE"))).thenReturn(Single.just(expectedTypes));

    // Act
    CompletionStage<ResponseEntity.Success<List<DataConnectorType>>> result =
        controller.listTypes("SOURCE");
    ResponseEntity.Success<List<DataConnectorType>> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(2, response.data().size());
    assertEquals("POSTGRES", response.data().get(0).getType());
    assertEquals("KAFKA", response.data().get(1).getType());
    verify(service).listTypes(eq("SOURCE"));
  }

  @Test
  public void listTypes_withSinkKind_returnsSinkTypes() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    List<DataConnectorType> expectedTypes =
        List.of(DataConnectorType.builder().id(1L).kind("SINK").type("S3").build());

    when(service.listTypes(eq("SINK"))).thenReturn(Single.just(expectedTypes));

    // Act
    CompletionStage<ResponseEntity.Success<List<DataConnectorType>>> result =
        controller.listTypes("SINK");
    ResponseEntity.Success<List<DataConnectorType>> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertEquals(1, response.data().size());
    assertEquals("SINK", response.data().get(0).getKind());
    verify(service).listTypes(eq("SINK"));
  }

  @Test
  public void listTypes_emptyList_returnsEmptyList() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    when(service.listTypes(eq("SOURCE"))).thenReturn(Single.just(Collections.emptyList()));

    // Act
    CompletionStage<ResponseEntity.Success<List<DataConnectorType>>> result =
        controller.listTypes("SOURCE");
    ResponseEntity.Success<List<DataConnectorType>> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertTrue(response.data().isEmpty());
  }

  @Test
  public void listTypes_serviceThrowsException_propagatesException() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    RuntimeException expectedException = new RuntimeException("Database error");
    when(service.listTypes(eq("SOURCE"))).thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<List<DataConnectorType>>> result =
        controller.listTypes("SOURCE");

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Database error"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // getConnectorTypeById Tests
  // ========================================

  @Test
  public void getConnectorTypeById_success_returnsConnectorType() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    DataConnectorType expectedType =
        DataConnectorType.builder()
            .id(TYPE_ID)
            .kind("SOURCE")
            .type("POSTGRES")
            .displayName("PostgreSQL Database")
            .configSchema(new JsonObject().put("type", "object"))
            .active(true)
            .build();

    when(service.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(expectedType));

    // Act
    CompletionStage<ResponseEntity.Success<DataConnectorType>> result =
        controller.getConnectorTypeById(TYPE_ID);
    ResponseEntity.Success<DataConnectorType> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(TYPE_ID, response.data().getId());
    assertEquals("POSTGRES", response.data().getType());
    verify(service).getConnectorTypeById(eq(TYPE_ID));
  }

  @Test
  public void getConnectorTypeById_notFound_propagatesException() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    RuntimeException expectedException = new RuntimeException("ConnectorType not found");
    when(service.getConnectorTypeById(eq(999L))).thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<DataConnectorType>> result =
        controller.getConnectorTypeById(999L);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("not found"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // onboardSource Tests
  // ========================================

  @Test
  public void onboardSource_success_returnsDataSourceDetails() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    OnboardDataSourceRequest request = new OnboardDataSourceRequest();
    request.setName("Production Database");
    request.setTypeId(TYPE_ID);
    request.setConfig(
        new JsonObject().put("host", "localhost").put("port", 5432).put("database", "prod_db"));

    DataSourceDetails expectedSource =
        DataSourceDetails.builder()
            .id(SOURCE_ID)
            .name("Production Database")
            .typeId(TYPE_ID)
            .type("POSTGRES")
            .config(request.getConfig())
            .status("ACTIVE")
            .createdBy(USER_EMAIL)
            .build();

    when(service.onboardSource(eq(request), eq(USER_EMAIL)))
        .thenReturn(Single.just(expectedSource));

    // Act
    CompletionStage<ResponseEntity.Success<DataSourceDetails>> result =
        controller.onboardSource(request, USER_EMAIL);
    ResponseEntity.Success<DataSourceDetails> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(SOURCE_ID, response.data().getId());
    assertEquals("Production Database", response.data().getName());
    assertEquals("POSTGRES", response.data().getType());
    assertEquals("ACTIVE", response.data().getStatus());
    verify(service).onboardSource(eq(request), eq(USER_EMAIL));
  }

  @Test
  public void onboardSource_invalidTypeId_propagatesException() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    OnboardDataSourceRequest request = new OnboardDataSourceRequest();
    request.setName("Test Source");
    request.setTypeId(999L);
    request.setConfig(new JsonObject());

    RuntimeException expectedException = new RuntimeException("ConnectorType not found: 999");
    when(service.onboardSource(eq(request), eq(USER_EMAIL)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<DataSourceDetails>> result =
        controller.onboardSource(request, USER_EMAIL);

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
  public void onboardSource_invalidConfig_propagatesException() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    OnboardDataSourceRequest request = new OnboardDataSourceRequest();
    request.setName("Test Source");
    request.setTypeId(TYPE_ID);
    request.setConfig(new JsonObject()); // Missing required fields

    RuntimeException expectedException = new RuntimeException("Config validation failed");
    when(service.onboardSource(eq(request), eq(USER_EMAIL)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<DataSourceDetails>> result =
        controller.onboardSource(request, USER_EMAIL);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(
          e.getCause().getMessage().contains("Config")
              || e.getCause().getMessage().contains("validation"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // onboardSink Tests
  // ========================================

  @Test
  public void onboardSink_success_returnsDataSinkDetails() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    OnboardDataSinkRequest request = new OnboardDataSinkRequest();
    request.setName("S3 Export Bucket");
    request.setTypeId(TYPE_ID);
    request.setConfig(
        new JsonObject().put("bucket", "my-export-bucket").put("region", "us-east-1"));

    DataSinkDetails expectedSink =
        DataSinkDetails.builder()
            .id(SINK_ID)
            .name("S3 Export Bucket")
            .typeId(TYPE_ID)
            .type("S3")
            .config(request.getConfig())
            .status("ACTIVE")
            .createdBy(USER_EMAIL)
            .build();

    when(service.onboardSink(eq(request), eq(USER_EMAIL))).thenReturn(Single.just(expectedSink));

    // Act
    CompletionStage<ResponseEntity.Success<DataSinkDetails>> result =
        controller.onboardSink(request, USER_EMAIL);
    ResponseEntity.Success<DataSinkDetails> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(SINK_ID, response.data().getId());
    assertEquals("S3 Export Bucket", response.data().getName());
    assertEquals("S3", response.data().getType());
    assertEquals("ACTIVE", response.data().getStatus());
    verify(service).onboardSink(eq(request), eq(USER_EMAIL));
  }

  @Test
  public void onboardSink_invalidTypeId_propagatesException() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    OnboardDataSinkRequest request = new OnboardDataSinkRequest();
    request.setName("Test Sink");
    request.setTypeId(999L);
    request.setConfig(new JsonObject());

    RuntimeException expectedException = new RuntimeException("ConnectorType not found: 999");
    when(service.onboardSink(eq(request), eq(USER_EMAIL)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<DataSinkDetails>> result =
        controller.onboardSink(request, USER_EMAIL);

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
  public void onboardSink_wrongConnectorKind_propagatesException() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    OnboardDataSinkRequest request = new OnboardDataSinkRequest();
    request.setName("Test Sink");
    request.setTypeId(TYPE_ID); // This is a SOURCE type, not SINK
    request.setConfig(new JsonObject());

    RuntimeException expectedException = new RuntimeException("Provided typeId is not a SINK");
    when(service.onboardSink(eq(request), eq(USER_EMAIL)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<DataSinkDetails>> result =
        controller.onboardSink(request, USER_EMAIL);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("SINK"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // listSources Tests
  // ========================================

  @Test
  public void listSources_success_returnsPaginatedResponse() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    List<DataSourceDetails> sources =
        List.of(
            DataSourceDetails.builder().id(1L).name("Source 1").type("POSTGRES").build(),
            DataSourceDetails.builder().id(2L).name("Source 2").type("KAFKA").build());

    PaginatedResponse<DataSourceDetails> expectedResponse =
        new PaginatedResponse<>(new PaginatedResponse.PageInfo(0, 10, false), sources);

    when(service.listSources(eq(0), eq(10))).thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSourceDetails>>> result =
        controller.listSources(10, 0);
    ResponseEntity.Success<PaginatedResponse<DataSourceDetails>> response =
        result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(2, response.data().data().size());
    assertEquals(0, response.data().pageInfo().page());
    assertEquals(10, response.data().pageInfo().pageSize());
    assertFalse(response.data().pageInfo().hasMore());
    verify(service).listSources(eq(0), eq(10));
  }

  @Test
  public void listSources_withPagination_returnsCorrectPage() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    List<DataSourceDetails> sources =
        List.of(
            DataSourceDetails.builder().id(11L).name("Source 11").build(),
            DataSourceDetails.builder().id(12L).name("Source 12").build(),
            DataSourceDetails.builder().id(13L).name("Source 13").build(),
            DataSourceDetails.builder().id(14L).name("Source 14").build(),
            DataSourceDetails.builder().id(15L).name("Source 15").build());

    PaginatedResponse<DataSourceDetails> expectedResponse =
        new PaginatedResponse<>(new PaginatedResponse.PageInfo(2, 5, true), sources);

    when(service.listSources(eq(2), eq(5))).thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSourceDetails>>> result =
        controller.listSources(5, 2);
    ResponseEntity.Success<PaginatedResponse<DataSourceDetails>> response =
        result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertEquals(5, response.data().data().size());
    assertEquals(2, response.data().pageInfo().page());
    assertTrue(response.data().pageInfo().hasMore());
  }

  @Test
  public void listSources_emptyList_returnsEmptyPaginatedResponse() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    PaginatedResponse<DataSourceDetails> expectedResponse =
        new PaginatedResponse<>(
            new PaginatedResponse.PageInfo(0, 10, false), Collections.emptyList());

    when(service.listSources(eq(0), eq(10))).thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSourceDetails>>> result =
        controller.listSources(10, 0);
    ResponseEntity.Success<PaginatedResponse<DataSourceDetails>> response =
        result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertTrue(response.data().data().isEmpty());
    assertFalse(response.data().pageInfo().hasMore());
  }

  @Test
  public void listSources_serviceThrowsException_propagatesException() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    RuntimeException expectedException = new RuntimeException("Database connection error");
    when(service.listSources(eq(0), eq(10))).thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSourceDetails>>> result =
        controller.listSources(10, 0);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Database"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // listSinks Tests
  // ========================================

  @Test
  public void listSinks_success_returnsPaginatedResponse() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    List<DataSinkDetails> sinks =
        List.of(
            DataSinkDetails.builder().id(1L).name("Sink 1").type("S3").build(),
            DataSinkDetails.builder().id(2L).name("Sink 2").type("WEBHOOK").build());

    PaginatedResponse<DataSinkDetails> expectedResponse =
        new PaginatedResponse<>(new PaginatedResponse.PageInfo(0, 10, false), sinks);

    when(service.listSinks(eq(0), eq(10))).thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSinkDetails>>> result =
        controller.listSinks(10, 0);
    ResponseEntity.Success<PaginatedResponse<DataSinkDetails>> response =
        result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(2, response.data().data().size());
    assertEquals(0, response.data().pageInfo().page());
    assertEquals(10, response.data().pageInfo().pageSize());
    assertFalse(response.data().pageInfo().hasMore());
    verify(service).listSinks(eq(0), eq(10));
  }

  @Test
  public void listSinks_withPagination_returnsCorrectPage() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    List<DataSinkDetails> sinks =
        List.of(
            DataSinkDetails.builder().id(6L).name("Sink 6").build(),
            DataSinkDetails.builder().id(7L).name("Sink 7").build(),
            DataSinkDetails.builder().id(8L).name("Sink 8").build());

    PaginatedResponse<DataSinkDetails> expectedResponse =
        new PaginatedResponse<>(new PaginatedResponse.PageInfo(1, 3, true), sinks);

    when(service.listSinks(eq(1), eq(3))).thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSinkDetails>>> result =
        controller.listSinks(3, 1);
    ResponseEntity.Success<PaginatedResponse<DataSinkDetails>> response =
        result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertEquals(3, response.data().data().size());
    assertEquals(1, response.data().pageInfo().page());
    assertTrue(response.data().pageInfo().hasMore());
  }

  @Test
  public void listSinks_emptyList_returnsEmptyPaginatedResponse() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    PaginatedResponse<DataSinkDetails> expectedResponse =
        new PaginatedResponse<>(
            new PaginatedResponse.PageInfo(0, 10, false), Collections.emptyList());

    when(service.listSinks(eq(0), eq(10))).thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSinkDetails>>> result =
        controller.listSinks(10, 0);
    ResponseEntity.Success<PaginatedResponse<DataSinkDetails>> response =
        result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertTrue(response.data().data().isEmpty());
    assertFalse(response.data().pageInfo().hasMore());
  }

  @Test
  public void listSinks_serviceThrowsException_propagatesException() {
    // Arrange
    DataConnectorService service = mock(DataConnectorService.class);
    DataSourcesController controller = buildController(service);

    RuntimeException expectedException = new RuntimeException("Database connection error");
    when(service.listSinks(eq(0), eq(10))).thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<PaginatedResponse<DataSinkDetails>>> result =
        controller.listSinks(10, 0);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Database"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }
}
