package io.ascend.flockr.admin.service.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.domain.dataconnectors.DataConnectorType;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.exception.ErrorEnum;
import io.ascend.flockr.admin.exception.ResourceNotFoundException;
import io.ascend.flockr.admin.io.request.OnboardConnectorTypeRequest;
import io.ascend.flockr.admin.io.request.OnboardDataSinkRequest;
import io.ascend.flockr.admin.io.request.OnboardDataSourceRequest;
import io.ascend.flockr.admin.io.response.PaginatedResponse;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.ascend.flockr.admin.service.DataConnectorService;
import io.ascend.flockr.admin.util.EncryptionUtils;
import io.ascend.flockr.admin.util.JsonUtil;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link DataConnectorService} providing data connector management operations.
 *
 * <p>This service handles:
 *
 * <ul>
 *   <li>Listing and retrieval of connector types
 *   <li>Onboarding data sources with schema validation
 *   <li>Onboarding data sinks with schema validation
 *   <li>Creating new connector types with JSON schema definitions
 *   <li>Pagination support for source and sink listings
 * </ul>
 *
 * <p>The service validates connector configurations against their respective JSON schemas before
 * persisting them to ensure data integrity.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DataConnectorServiceImpl implements DataConnectorService {

  // Connector kind constants
  private static final String KIND_SOURCE = "SOURCE";
  private static final String KIND_SINK = "SINK";

  // Status constants
  private static final String STATUS_ACTIVE = "ACTIVE";

  // Config field keys
  private static final String CONFIG_CONNECTOR_TYPE = "connectorType";

  // Error messages
  private static final String ERROR_NOT_SOURCE = "Provided typeId %d is not a SOURCE";
  private static final String ERROR_NOT_SINK = "Provided typeId %d is not a SINK";
  private static final String ERROR_INVALID_KIND = "Kind must be either 'SOURCE' or 'SINK'";

  // Resource name for exceptions
  private static final String RESOURCE_CONNECTOR_TYPE = "ConnectorType";

  private final DataConnectorRepository repository;

  /**
   * {@inheritDoc}
   *
   * <p>This implementation delegates to the repository to fetch all active connector types of the
   * specified kind.
   */
  @Override
  public Single<List<DataConnectorType>> listTypes(String kind) {
    return repository.listConnectorTypes(kind);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation validates the config against the connector type's JSON schema before
   * persisting.
   */
  @Override
  public Single<DataSourceDetails> onboardSource(
      OnboardDataSourceRequest request, String createdBy) {
    return repository
        .getConnectorTypeById(request.getTypeId())
        .onErrorResumeNext(
            error -> {
              if (error instanceof NoSuchElementException) {
                return Single.error(
                    new ResourceNotFoundException(RESOURCE_CONNECTOR_TYPE, request.getTypeId()));
              }
              return Single.error(error);
            })
        .flatMap(
            type -> {
              validateConnectorKind(type, KIND_SOURCE, request.getTypeId());
              validateConfigAgainstSchema(request.getConfig(), type);
              request.getConfig().put(CONFIG_CONNECTOR_TYPE, type.getType());
              JsonObject configForStorage = EncryptionUtils.processForStorage(request.getConfig());
              return repository
                  .createDataSource(
                      request.getName(), request.getTypeId(), createdBy, configForStorage)
                  .map(
                      id -> {
                        JsonObject plaintextConfig =
                            EncryptionUtils.processFromStorage(configForStorage);
                        return DataSourceDetails.builder()
                            .id(id)
                            .name(request.getName())
                            .typeId(request.getTypeId())
                            .type(type.getType())
                            .config(plaintextConfig)
                            .status(STATUS_ACTIVE)
                            .createdBy(createdBy)
                            .build();
                      });
            });
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation validates the config against the connector type's JSON schema before
   * persisting.
   */
  @Override
  public Single<DataSinkDetails> onboardSink(OnboardDataSinkRequest request, String createdBy) {
    return repository
        .getConnectorTypeById(request.getTypeId())
        .onErrorResumeNext(
            error -> {
              if (error instanceof NoSuchElementException) {
                return Single.error(
                    new ResourceNotFoundException(RESOURCE_CONNECTOR_TYPE, request.getTypeId()));
              }
              return Single.error(error);
            })
        .flatMap(
            type -> {
              validateConnectorKind(type, KIND_SINK, request.getTypeId());
              validateConfigAgainstSchema(request.getConfig(), type);
              request.getConfig().put(CONFIG_CONNECTOR_TYPE, type.getType());
              JsonObject configForStorage = EncryptionUtils.processForStorage(request.getConfig());

              return repository
                  .createDataSink(
                      request.getName(), request.getTypeId(), createdBy, configForStorage)
                  .map(
                      id -> {
                        JsonObject plaintextConfig =
                            EncryptionUtils.processFromStorage(configForStorage);

                        return DataSinkDetails.builder()
                            .id(id)
                            .name(request.getName())
                            .typeId(request.getTypeId())
                            .type(type.getType())
                            .config(plaintextConfig)
                            .status(STATUS_ACTIVE)
                            .createdBy(createdBy)
                            .build();
                      });
            });
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation wraps the repository results in a paginated response with hasMore
   * detection.
   */
  @Override
  public Single<PaginatedResponse<DataSourceDetails>> listSources(int page, int pageSize) {
    return repository
        .listDataSources(page, pageSize)
        .map(
            list ->
                new PaginatedResponse<>(
                    new PaginatedResponse.PageInfo(page, pageSize, list.size() == pageSize), list));
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation wraps the repository results in a paginated response with hasMore
   * detection.
   */
  @Override
  public Single<PaginatedResponse<DataSinkDetails>> listSinks(int page, int pageSize) {
    return repository
        .listDataSinks(page, pageSize)
        .map(
            list ->
                new PaginatedResponse<>(
                    new PaginatedResponse.PageInfo(page, pageSize, list.size() == pageSize), list));
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation validates that kind is either SOURCE or SINK before persisting.
   */
  @Override
  public Single<DataConnectorType> onboardConnectorType(
      OnboardConnectorTypeRequest request, String createdBy) {
    String kind = request.getKind().toUpperCase();
    if (!KIND_SOURCE.equals(kind) && !KIND_SINK.equals(kind)) {
      return Single.error(ErrorEnum.INVALID_ARGUMENT.toException(ERROR_INVALID_KIND));
    }

    JsonObject configSchemaJson = request.getConfigSchema();

    return repository
        .createConnectorType(
            kind, request.getType(), request.getDisplayName(), createdBy, configSchemaJson)
        .flatMap(
            typeId ->
                repository
                    .getConnectorTypeById(typeId)
                    .onErrorResumeNext(
                        error -> {
                          if (error instanceof NoSuchElementException) {
                            return Single.error(
                                new ResourceNotFoundException(RESOURCE_CONNECTOR_TYPE, typeId));
                          }
                          return Single.error(error);
                        }));
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation delegates directly to the repository with proper not found handling.
   */
  @Override
  public Single<DataConnectorType> getConnectorTypeById(Long typeId) {
    return repository
        .getConnectorTypeById(typeId)
        .onErrorResumeNext(
            error -> {
              if (error instanceof NoSuchElementException) {
                return Single.error(new ResourceNotFoundException(RESOURCE_CONNECTOR_TYPE, typeId));
              }
              return Single.error(error);
            });
  }

  /**
   * Validates that a connector type matches the expected kind.
   *
   * @param type the connector type to validate
   * @param expectedKind the expected kind (SOURCE or SINK)
   * @param typeId the connector type ID for error messages
   * @throws RuntimeException if kind doesn't match
   */
  private void validateConnectorKind(DataConnectorType type, String expectedKind, Long typeId) {
    if (!expectedKind.equalsIgnoreCase(type.getKind())) {
      String errorMessage =
          KIND_SOURCE.equals(expectedKind)
              ? String.format(ERROR_NOT_SOURCE, typeId)
              : String.format(ERROR_NOT_SINK, typeId);
      throw ErrorEnum.INVALID_ARGUMENT.toException(errorMessage);
    }
  }

  /**
   * Validates a configuration JsonObject against the connector type's JSON Schema.
   *
   * @param config the configuration to validate
   * @param connectorType the connector type containing the schema
   */
  private void validateConfigAgainstSchema(JsonObject config, DataConnectorType connectorType) {
    JsonObject schema = connectorType.getConfigSchema();

    if (schema != null && !schema.isEmpty()) {
      log.debug(
          "Validating config against schema for connector type: {} (ID: {})",
          connectorType.getType(),
          connectorType.getId());
      JsonUtil.validateAgainstSchema(config, schema);
    } else {
      log.warn(
          "No schema found for connector type ID: {}, skipping schema validation",
          connectorType.getId());
    }
  }
}
