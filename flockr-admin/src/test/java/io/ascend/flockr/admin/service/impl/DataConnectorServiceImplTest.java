package io.ascend.flockr.admin.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dream11.rest.exception.RestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.ascend.flockr.admin.domain.dataconnectors.DataConnectorType;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.exception.ResourceNotFoundException;
import io.ascend.flockr.admin.io.request.OnboardConnectorTypeRequest;
import io.ascend.flockr.admin.io.request.OnboardDataSinkRequest;
import io.ascend.flockr.admin.io.request.OnboardDataSourceRequest;
import io.ascend.flockr.admin.io.response.PaginatedResponse;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.ascend.flockr.admin.util.JsonUtil;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.json.JsonObject;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link DataConnectorServiceImpl}.
 *
 * <p>These tests verify the service layer business logic including connector type management, data
 * source onboarding, data sink onboarding, and pagination.
 */
public class DataConnectorServiceImplTest {

  private static final String USER_EMAIL = "user@example.com";
  private static final Long TYPE_ID = 1L;
  private static final Long SOURCE_ID = 100L;
  private static final Long SINK_ID = 200L;

  /**
   * Initialize the static fields in JsonUtil before running tests. This is necessary because the
   * utility class uses Guice @Inject on static fields which aren't available in unit tests.
   */
  @BeforeClass
  public static void setupSchemaValidator() throws Exception {
    // Initialize ObjectMapper
    Field objectMapperField = JsonUtil.class.getDeclaredField("objectMapper");
    objectMapperField.setAccessible(true);
    objectMapperField.set(null, new ObjectMapper());

    // Initialize JsonSchemaFactory
    Field schemaFactoryField = JsonUtil.class.getDeclaredField("schemaFactory");
    schemaFactoryField.setAccessible(true);
    schemaFactoryField.set(null, JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4));
  }

  private DataConnectorServiceImpl buildService(DataConnectorRepository repository) {
    return new DataConnectorServiceImpl(repository);
  }

  private static DataConnectorType buildSourceConnectorType() {
    return DataConnectorType.builder()
        .id(TYPE_ID)
        .kind("SOURCE")
        .type("POSTGRES")
        .displayName("PostgreSQL")
        .configSchema(
            new JsonObject()
                .put("type", "object")
                .put(
                    "properties",
                    new JsonObject()
                        .put("host", new JsonObject().put("type", "string"))
                        .put("port", new JsonObject().put("type", "integer"))))
        .active(true)
        .build();
  }

  private static DataConnectorType buildSinkConnectorType() {
    return DataConnectorType.builder()
        .id(TYPE_ID)
        .kind("SINK")
        .type("S3")
        .displayName("Amazon S3")
        .configSchema(
            new JsonObject()
                .put("type", "object")
                .put(
                    "properties",
                    new JsonObject()
                        .put("bucket", new JsonObject().put("type", "string"))
                        .put("region", new JsonObject().put("type", "string"))))
        .active(true)
        .build();
  }

  // ========================================
  // listTypes Tests
  // ========================================

  @Test
  public void listTypes_success_returnsTypesList() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    List<DataConnectorType> expectedTypes =
        List.of(
            DataConnectorType.builder().id(1L).kind("SOURCE").type("POSTGRES").build(),
            DataConnectorType.builder().id(2L).kind("SOURCE").type("KAFKA").build());

    when(repository.listConnectorTypes(eq("SOURCE"))).thenReturn(Single.just(expectedTypes));

    // Act
    TestObserver<List<DataConnectorType>> to = service.listTypes("SOURCE").test();

    // Assert
    to.assertComplete();
    to.assertValue(
        types -> {
          assertEquals(2, types.size());
          assertEquals("POSTGRES", types.get(0).getType());
          assertEquals("KAFKA", types.get(1).getType());
          return true;
        });
    verify(repository).listConnectorTypes(eq("SOURCE"));
  }

  @Test
  public void listTypes_emptyList_returnsEmptyList() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    when(repository.listConnectorTypes(eq("SOURCE")))
        .thenReturn(Single.just(Collections.emptyList()));

    // Act
    TestObserver<List<DataConnectorType>> to = service.listTypes("SOURCE").test();

    // Assert
    to.assertComplete();
    to.assertValue(List::isEmpty);
  }

  @Test
  public void listTypes_repositoryError_propagatesException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    when(repository.listConnectorTypes(eq("SOURCE")))
        .thenReturn(Single.error(new RuntimeException("Database error")));

    // Act
    TestObserver<List<DataConnectorType>> to = service.listTypes("SOURCE").test();

    // Assert
    to.assertError(RuntimeException.class);
  }

  // ========================================
  // getConnectorTypeById Tests
  // ========================================

  @Test
  public void getConnectorTypeById_success_returnsConnectorType() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    DataConnectorType expectedType = buildSourceConnectorType();
    when(repository.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(expectedType));

    // Act
    TestObserver<DataConnectorType> to = service.getConnectorTypeById(TYPE_ID).test();

    // Assert
    to.assertComplete();
    to.assertValue(
        type -> {
          assertEquals(TYPE_ID, type.getId());
          assertEquals("POSTGRES", type.getType());
          assertEquals("SOURCE", type.getKind());
          return true;
        });
  }

  @Test
  public void getConnectorTypeById_notFound_throwsResourceNotFoundException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    when(repository.getConnectorTypeById(eq(999L)))
        .thenReturn(Single.error(new NoSuchElementException("Not found")));

    // Act
    TestObserver<DataConnectorType> to = service.getConnectorTypeById(999L).test();

    // Assert
    to.assertError(ResourceNotFoundException.class);
  }

  @Test
  public void getConnectorTypeById_otherError_propagatesException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    when(repository.getConnectorTypeById(eq(TYPE_ID)))
        .thenReturn(Single.error(new RuntimeException("Database error")));

    // Act
    TestObserver<DataConnectorType> to = service.getConnectorTypeById(TYPE_ID).test();

    // Assert
    to.assertError(RuntimeException.class);
  }

  // ========================================
  // onboardConnectorType Tests
  // ========================================

  @Test
  public void onboardConnectorType_success_returnsCreatedType() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardConnectorTypeRequest request = new OnboardConnectorTypeRequest();
    request.setKind("SOURCE");
    request.setType("ATHENA");
    request.setDisplayName("Amazon Athena");
    request.setConfigSchema(new JsonObject().put("type", "object"));

    Long createdTypeId = 10L;
    DataConnectorType createdType =
        DataConnectorType.builder()
            .id(createdTypeId)
            .kind("SOURCE")
            .type("ATHENA")
            .displayName("Amazon Athena")
            .configSchema(request.getConfigSchema())
            .active(true)
            .build();

    when(repository.createConnectorType(
            eq("SOURCE"), eq("ATHENA"), eq("Amazon Athena"), eq(USER_EMAIL), any(JsonObject.class)))
        .thenReturn(Single.just(createdTypeId));
    when(repository.getConnectorTypeById(eq(createdTypeId))).thenReturn(Single.just(createdType));

    // Act
    TestObserver<DataConnectorType> to = service.onboardConnectorType(request, USER_EMAIL).test();

    // Assert
    to.assertComplete();
    to.assertValue(
        type -> {
          assertEquals(createdTypeId, type.getId());
          assertEquals("ATHENA", type.getType());
          assertEquals("Amazon Athena", type.getDisplayName());
          return true;
        });
  }

  @Test
  public void onboardConnectorType_withSinkKind_success() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardConnectorTypeRequest request = new OnboardConnectorTypeRequest();
    request.setKind("SINK");
    request.setType("WEBHOOK");
    request.setDisplayName("Webhook Export");
    request.setConfigSchema(new JsonObject().put("type", "object"));

    Long createdTypeId = 11L;
    DataConnectorType createdType =
        DataConnectorType.builder()
            .id(createdTypeId)
            .kind("SINK")
            .type("WEBHOOK")
            .displayName("Webhook Export")
            .active(true)
            .build();

    when(repository.createConnectorType(
            eq("SINK"), eq("WEBHOOK"), eq("Webhook Export"), eq(USER_EMAIL), any(JsonObject.class)))
        .thenReturn(Single.just(createdTypeId));
    when(repository.getConnectorTypeById(eq(createdTypeId))).thenReturn(Single.just(createdType));

    // Act
    TestObserver<DataConnectorType> to = service.onboardConnectorType(request, USER_EMAIL).test();

    // Assert
    to.assertComplete();
    to.assertValue(type -> "SINK".equals(type.getKind()));
  }

  @Test
  public void onboardConnectorType_invalidKind_throwsException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardConnectorTypeRequest request = new OnboardConnectorTypeRequest();
    request.setKind("INVALID");
    request.setType("POSTGRES");
    request.setDisplayName("PostgreSQL");
    request.setConfigSchema(new JsonObject());

    // Act
    TestObserver<DataConnectorType> to = service.onboardConnectorType(request, USER_EMAIL).test();

    // Assert
    to.assertError(RestException.class);
  }

  @Test
  public void onboardConnectorType_lowercaseKind_normalizedToUppercase() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardConnectorTypeRequest request = new OnboardConnectorTypeRequest();
    request.setKind("source"); // lowercase
    request.setType("MYSQL");
    request.setDisplayName("MySQL");
    request.setConfigSchema(new JsonObject());

    Long createdTypeId = 12L;
    DataConnectorType createdType =
        DataConnectorType.builder().id(createdTypeId).kind("SOURCE").type("MYSQL").build();

    when(repository.createConnectorType(
            eq("SOURCE"), eq("MYSQL"), eq("MySQL"), eq(USER_EMAIL), any(JsonObject.class)))
        .thenReturn(Single.just(createdTypeId));
    when(repository.getConnectorTypeById(eq(createdTypeId))).thenReturn(Single.just(createdType));

    // Act
    TestObserver<DataConnectorType> to = service.onboardConnectorType(request, USER_EMAIL).test();

    // Assert
    to.assertComplete();
    verify(repository)
        .createConnectorType(
            eq("SOURCE"), eq("MYSQL"), eq("MySQL"), eq(USER_EMAIL), any(JsonObject.class));
  }

  @Test
  public void onboardConnectorType_repositoryError_propagatesException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardConnectorTypeRequest request = new OnboardConnectorTypeRequest();
    request.setKind("SOURCE");
    request.setType("POSTGRES");
    request.setDisplayName("PostgreSQL");
    request.setConfigSchema(new JsonObject());

    when(repository.createConnectorType(
            anyString(), anyString(), anyString(), anyString(), any(JsonObject.class)))
        .thenReturn(Single.error(new RuntimeException("Database error")));

    // Act
    TestObserver<DataConnectorType> to = service.onboardConnectorType(request, USER_EMAIL).test();

    // Assert
    to.assertError(RuntimeException.class);
  }

  // ========================================
  // onboardSource Tests
  // ========================================

  @Test
  public void onboardSource_success_returnsDataSourceDetails() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardDataSourceRequest request = new OnboardDataSourceRequest();
    request.setName("Production DB");
    request.setTypeId(TYPE_ID);
    request.setConfig(new JsonObject().put("host", "localhost").put("port", 5432));

    DataConnectorType connectorType = buildSourceConnectorType();

    when(repository.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(connectorType));
    when(repository.createDataSource(
            eq("Production DB"), eq(TYPE_ID), eq(USER_EMAIL), any(JsonObject.class)))
        .thenReturn(Single.just(SOURCE_ID));

    // Act
    TestObserver<DataSourceDetails> to = service.onboardSource(request, USER_EMAIL).test();

    // Assert
    to.assertComplete();
    to.assertValue(
        source -> {
          assertEquals(SOURCE_ID, source.getId());
          assertEquals("Production DB", source.getName());
          assertEquals(TYPE_ID, source.getTypeId());
          assertEquals("POSTGRES", source.getType());
          assertEquals("ACTIVE", source.getStatus());
          assertEquals(USER_EMAIL, source.getCreatedBy());
          return true;
        });
  }

  @Test
  public void onboardSource_typeNotFound_throwsResourceNotFoundException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardDataSourceRequest request = new OnboardDataSourceRequest();
    request.setName("Test Source");
    request.setTypeId(999L);
    request.setConfig(new JsonObject());

    when(repository.getConnectorTypeById(eq(999L)))
        .thenReturn(Single.error(new NoSuchElementException("Not found")));

    // Act
    TestObserver<DataSourceDetails> to = service.onboardSource(request, USER_EMAIL).test();

    // Assert
    to.assertError(ResourceNotFoundException.class);
  }

  @Test
  public void onboardSource_wrongConnectorKind_throwsException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardDataSourceRequest request = new OnboardDataSourceRequest();
    request.setName("Test Source");
    request.setTypeId(TYPE_ID);
    request.setConfig(new JsonObject());

    // Return a SINK type instead of SOURCE
    DataConnectorType sinkType = buildSinkConnectorType();
    when(repository.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(sinkType));

    // Act
    TestObserver<DataSourceDetails> to = service.onboardSource(request, USER_EMAIL).test();

    // Assert
    to.assertError(RestException.class);
  }

  @Test
  public void onboardSource_addsConnectorTypeToConfig() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    JsonObject config = new JsonObject().put("host", "localhost");
    OnboardDataSourceRequest request = new OnboardDataSourceRequest();
    request.setName("Production DB");
    request.setTypeId(TYPE_ID);
    request.setConfig(config);

    DataConnectorType connectorType = buildSourceConnectorType();

    when(repository.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(connectorType));
    when(repository.createDataSource(anyString(), anyLong(), anyString(), any(JsonObject.class)))
        .thenReturn(Single.just(SOURCE_ID));

    // Act
    service.onboardSource(request, USER_EMAIL).test().assertComplete();

    // Assert - verify connectorType was added to config
    assertEquals("POSTGRES", request.getConfig().getString("connectorType"));
  }

  @Test
  public void onboardSource_repositoryError_propagatesException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardDataSourceRequest request = new OnboardDataSourceRequest();
    request.setName("Test Source");
    request.setTypeId(TYPE_ID);
    request.setConfig(new JsonObject());

    DataConnectorType connectorType = buildSourceConnectorType();

    when(repository.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(connectorType));
    when(repository.createDataSource(anyString(), anyLong(), anyString(), any(JsonObject.class)))
        .thenReturn(Single.error(new RuntimeException("Database error")));

    // Act
    TestObserver<DataSourceDetails> to = service.onboardSource(request, USER_EMAIL).test();

    // Assert
    to.assertError(RuntimeException.class);
  }

  // ========================================
  // onboardSink Tests
  // ========================================

  @Test
  public void onboardSink_success_returnsDataSinkDetails() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardDataSinkRequest request = new OnboardDataSinkRequest();
    request.setName("Export Bucket");
    request.setTypeId(TYPE_ID);
    request.setConfig(new JsonObject().put("bucket", "my-bucket").put("region", "us-east-1"));

    DataConnectorType connectorType = buildSinkConnectorType();

    when(repository.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(connectorType));
    when(repository.createDataSink(
            eq("Export Bucket"), eq(TYPE_ID), eq(USER_EMAIL), any(JsonObject.class)))
        .thenReturn(Single.just(SINK_ID));

    // Act
    TestObserver<DataSinkDetails> to = service.onboardSink(request, USER_EMAIL).test();

    // Assert
    to.assertComplete();
    to.assertValue(
        sink -> {
          assertEquals(SINK_ID, sink.getId());
          assertEquals("Export Bucket", sink.getName());
          assertEquals(TYPE_ID, sink.getTypeId());
          assertEquals("S3", sink.getType());
          assertEquals("ACTIVE", sink.getStatus());
          assertEquals(USER_EMAIL, sink.getCreatedBy());
          return true;
        });
  }

  @Test
  public void onboardSink_typeNotFound_throwsResourceNotFoundException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardDataSinkRequest request = new OnboardDataSinkRequest();
    request.setName("Test Sink");
    request.setTypeId(999L);
    request.setConfig(new JsonObject());

    when(repository.getConnectorTypeById(eq(999L)))
        .thenReturn(Single.error(new NoSuchElementException("Not found")));

    // Act
    TestObserver<DataSinkDetails> to = service.onboardSink(request, USER_EMAIL).test();

    // Assert
    to.assertError(ResourceNotFoundException.class);
  }

  @Test
  public void onboardSink_wrongConnectorKind_throwsException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardDataSinkRequest request = new OnboardDataSinkRequest();
    request.setName("Test Sink");
    request.setTypeId(TYPE_ID);
    request.setConfig(new JsonObject());

    // Return a SOURCE type instead of SINK
    DataConnectorType sourceType = buildSourceConnectorType();
    when(repository.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(sourceType));

    // Act
    TestObserver<DataSinkDetails> to = service.onboardSink(request, USER_EMAIL).test();

    // Assert
    to.assertError(RestException.class);
  }

  @Test
  public void onboardSink_addsConnectorTypeToConfig() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    JsonObject config = new JsonObject().put("bucket", "my-bucket");
    OnboardDataSinkRequest request = new OnboardDataSinkRequest();
    request.setName("Export Bucket");
    request.setTypeId(TYPE_ID);
    request.setConfig(config);

    DataConnectorType connectorType = buildSinkConnectorType();

    when(repository.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(connectorType));
    when(repository.createDataSink(anyString(), anyLong(), anyString(), any(JsonObject.class)))
        .thenReturn(Single.just(SINK_ID));

    // Act
    service.onboardSink(request, USER_EMAIL).test().assertComplete();

    // Assert - verify connectorType was added to config
    assertEquals("S3", request.getConfig().getString("connectorType"));
  }

  @Test
  public void onboardSink_repositoryError_propagatesException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardDataSinkRequest request = new OnboardDataSinkRequest();
    request.setName("Test Sink");
    request.setTypeId(TYPE_ID);
    request.setConfig(new JsonObject());

    DataConnectorType connectorType = buildSinkConnectorType();

    when(repository.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(connectorType));
    when(repository.createDataSink(anyString(), anyLong(), anyString(), any(JsonObject.class)))
        .thenReturn(Single.error(new RuntimeException("Database error")));

    // Act
    TestObserver<DataSinkDetails> to = service.onboardSink(request, USER_EMAIL).test();

    // Assert
    to.assertError(RuntimeException.class);
  }

  // ========================================
  // listSources Tests
  // ========================================

  @Test
  public void listSources_success_returnsPaginatedResponse() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    List<DataSourceDetails> sources =
        List.of(
            DataSourceDetails.builder().id(1L).name("Source 1").type("POSTGRES").build(),
            DataSourceDetails.builder().id(2L).name("Source 2").type("KAFKA").build());

    when(repository.listDataSources(eq(0), eq(10))).thenReturn(Single.just(sources));

    // Act
    TestObserver<PaginatedResponse<DataSourceDetails>> to = service.listSources(0, 10).test();

    // Assert
    to.assertComplete();
    to.assertValue(
        response -> {
          assertEquals(2, response.data().size());
          assertEquals(0, response.pageInfo().page());
          assertEquals(10, response.pageInfo().pageSize());
          assertFalse(response.pageInfo().hasMore()); // 2 < 10
          return true;
        });
  }

  @Test
  public void listSources_exactPageSize_hasMoreIsTrue() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    List<DataSourceDetails> sources =
        List.of(
            DataSourceDetails.builder().id(1L).build(),
            DataSourceDetails.builder().id(2L).build(),
            DataSourceDetails.builder().id(3L).build());

    when(repository.listDataSources(eq(0), eq(3))).thenReturn(Single.just(sources));

    // Act
    TestObserver<PaginatedResponse<DataSourceDetails>> to = service.listSources(0, 3).test();

    // Assert
    to.assertComplete();
    to.assertValue(response -> response.pageInfo().hasMore());
  }

  @Test
  public void listSources_lessThanPageSize_hasMoreIsFalse() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    List<DataSourceDetails> sources = List.of(DataSourceDetails.builder().id(1L).build());

    when(repository.listDataSources(eq(0), eq(10))).thenReturn(Single.just(sources));

    // Act
    TestObserver<PaginatedResponse<DataSourceDetails>> to = service.listSources(0, 10).test();

    // Assert
    to.assertComplete();
    to.assertValue(response -> !response.pageInfo().hasMore());
  }

  @Test
  public void listSources_emptyList_returnsEmptyPaginatedResponse() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    when(repository.listDataSources(eq(0), eq(10)))
        .thenReturn(Single.just(Collections.emptyList()));

    // Act
    TestObserver<PaginatedResponse<DataSourceDetails>> to = service.listSources(0, 10).test();

    // Assert
    to.assertComplete();
    to.assertValue(
        response -> {
          assertTrue(response.data().isEmpty());
          assertFalse(response.pageInfo().hasMore());
          return true;
        });
  }

  @Test
  public void listSources_repositoryError_propagatesException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    when(repository.listDataSources(eq(0), eq(10)))
        .thenReturn(Single.error(new RuntimeException("Database error")));

    // Act
    TestObserver<PaginatedResponse<DataSourceDetails>> to = service.listSources(0, 10).test();

    // Assert
    to.assertError(RuntimeException.class);
  }

  // ========================================
  // listSinks Tests
  // ========================================

  @Test
  public void listSinks_success_returnsPaginatedResponse() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    List<DataSinkDetails> sinks =
        List.of(
            DataSinkDetails.builder().id(1L).name("Sink 1").type("S3").build(),
            DataSinkDetails.builder().id(2L).name("Sink 2").type("WEBHOOK").build());

    when(repository.listDataSinks(eq(0), eq(10))).thenReturn(Single.just(sinks));

    // Act
    TestObserver<PaginatedResponse<DataSinkDetails>> to = service.listSinks(0, 10).test();

    // Assert
    to.assertComplete();
    to.assertValue(
        response -> {
          assertEquals(2, response.data().size());
          assertEquals(0, response.pageInfo().page());
          assertEquals(10, response.pageInfo().pageSize());
          assertFalse(response.pageInfo().hasMore()); // 2 < 10
          return true;
        });
  }

  @Test
  public void listSinks_exactPageSize_hasMoreIsTrue() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    List<DataSinkDetails> sinks =
        List.of(
            DataSinkDetails.builder().id(1L).build(),
            DataSinkDetails.builder().id(2L).build(),
            DataSinkDetails.builder().id(3L).build(),
            DataSinkDetails.builder().id(4L).build(),
            DataSinkDetails.builder().id(5L).build());

    when(repository.listDataSinks(eq(1), eq(5))).thenReturn(Single.just(sinks));

    // Act
    TestObserver<PaginatedResponse<DataSinkDetails>> to = service.listSinks(1, 5).test();

    // Assert
    to.assertComplete();
    to.assertValue(response -> response.pageInfo().hasMore());
  }

  @Test
  public void listSinks_lessThanPageSize_hasMoreIsFalse() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    List<DataSinkDetails> sinks =
        List.of(DataSinkDetails.builder().id(1L).build(), DataSinkDetails.builder().id(2L).build());

    when(repository.listDataSinks(eq(0), eq(10))).thenReturn(Single.just(sinks));

    // Act
    TestObserver<PaginatedResponse<DataSinkDetails>> to = service.listSinks(0, 10).test();

    // Assert
    to.assertComplete();
    to.assertValue(response -> !response.pageInfo().hasMore());
  }

  @Test
  public void listSinks_emptyList_returnsEmptyPaginatedResponse() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    when(repository.listDataSinks(eq(0), eq(10))).thenReturn(Single.just(Collections.emptyList()));

    // Act
    TestObserver<PaginatedResponse<DataSinkDetails>> to = service.listSinks(0, 10).test();

    // Assert
    to.assertComplete();
    to.assertValue(
        response -> {
          assertTrue(response.data().isEmpty());
          assertFalse(response.pageInfo().hasMore());
          return true;
        });
  }

  @Test
  public void listSinks_repositoryError_propagatesException() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    when(repository.listDataSinks(eq(0), eq(10)))
        .thenReturn(Single.error(new RuntimeException("Database error")));

    // Act
    TestObserver<PaginatedResponse<DataSinkDetails>> to = service.listSinks(0, 10).test();

    // Assert
    to.assertError(RuntimeException.class);
  }

  // ========================================
  // Schema Validation Tests
  // ========================================

  @Test
  public void onboardSource_withNullSchema_skipValidation() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardDataSourceRequest request = new OnboardDataSourceRequest();
    request.setName("Test Source");
    request.setTypeId(TYPE_ID);
    request.setConfig(new JsonObject().put("anyField", "anyValue"));

    // Connector type without schema
    DataConnectorType connectorType =
        DataConnectorType.builder()
            .id(TYPE_ID)
            .kind("SOURCE")
            .type("CUSTOM")
            .configSchema(null)
            .active(true)
            .build();

    when(repository.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(connectorType));
    when(repository.createDataSource(anyString(), anyLong(), anyString(), any(JsonObject.class)))
        .thenReturn(Single.just(SOURCE_ID));

    // Act
    TestObserver<DataSourceDetails> to = service.onboardSource(request, USER_EMAIL).test();

    // Assert - should complete successfully even without schema validation
    to.assertComplete();
  }

  @Test
  public void onboardSource_withEmptySchema_skipValidation() {
    // Arrange
    DataConnectorRepository repository = mock(DataConnectorRepository.class);
    DataConnectorServiceImpl service = buildService(repository);

    OnboardDataSourceRequest request = new OnboardDataSourceRequest();
    request.setName("Test Source");
    request.setTypeId(TYPE_ID);
    request.setConfig(new JsonObject().put("anyField", "anyValue"));

    // Connector type with empty schema
    DataConnectorType connectorType =
        DataConnectorType.builder()
            .id(TYPE_ID)
            .kind("SOURCE")
            .type("CUSTOM")
            .configSchema(new JsonObject())
            .active(true)
            .build();

    when(repository.getConnectorTypeById(eq(TYPE_ID))).thenReturn(Single.just(connectorType));
    when(repository.createDataSource(anyString(), anyLong(), anyString(), any(JsonObject.class)))
        .thenReturn(Single.just(SOURCE_ID));

    // Act
    TestObserver<DataSourceDetails> to = service.onboardSource(request, USER_EMAIL).test();

    // Assert - should complete successfully even without schema validation
    to.assertComplete();
  }
}
