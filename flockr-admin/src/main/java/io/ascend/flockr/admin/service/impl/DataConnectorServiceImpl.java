package io.ascend.flockr.admin.service.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.domain.dataconnectors.DataConnectorType;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.io.request.OnboardConnectorTypeRequest;
import io.ascend.flockr.admin.io.request.OnboardDataSinkRequest;
import io.ascend.flockr.admin.io.request.OnboardDataSourceRequest;
import io.ascend.flockr.admin.io.response.PaginatedResponse;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.ascend.flockr.admin.service.DataConnectorService;
import io.ascend.flockr.admin.util.json.JsonSchemaValidationUtil;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import java.util.List;
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
        .map(
            type -> {
              if (!"SOURCE".equalsIgnoreCase(type.getKind())) {
                throw new IllegalArgumentException("Provided typeId is not a SOURCE");
              }
              validateConfigAgainstSchema(request.getConfig(), type);
              request.getConfig().put("connectorType", type.getType());
              return type;
            })
        .flatMap(
            type ->
                repository
                    .createDataSource(
                        request.getName(), request.getTypeId(), createdBy, request.getConfig())
                    .map(
                        id ->
                            DataSourceDetails.builder()
                                .id(id)
                                .name(request.getName())
                                .typeId(request.getTypeId())
                                .type(type.getType())
                                .config(request.getConfig())
                                .status("ACTIVE")
                                .createdBy(createdBy)
                                .build()));
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
        .map(
            type -> {
              if (!"SINK".equalsIgnoreCase(type.getKind())) {
                throw new IllegalArgumentException("Provided typeId is not a SINK");
              }
              validateConfigAgainstSchema(request.getConfig(), type);
              request.getConfig().put("connectorType", type.getType());
              return type;
            })
        .flatMap(
            type ->
                repository
                    .createDataSink(
                        request.getName(), request.getTypeId(), createdBy, request.getConfig())
                    .map(
                        id ->
                            DataSinkDetails.builder()
                                .id(id)
                                .name(request.getName())
                                .typeId(request.getTypeId())
                                .type(type.getType())
                                .config(request.getConfig())
                                .status("ACTIVE")
                                .createdBy(createdBy)
                                .build()));
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
    // Validate kind is either SOURCE or SINK
    String kind = request.getKind().toUpperCase();
    if (!"SOURCE".equals(kind) && !"SINK".equals(kind)) {
      return Single.error(new IllegalArgumentException("Kind must be either 'SOURCE' or 'SINK'"));
    }

    JsonObject configSchemaJson = request.getConfigSchema();

    return repository
        .createConnectorType(
            kind, request.getType(), request.getDisplayName(), createdBy, configSchemaJson)
        .flatMap(repository::getConnectorTypeById);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation delegates directly to the repository.
   */
  @Override
  public Single<DataConnectorType> getConnectorTypeById(Long typeId) {
    return repository.getConnectorTypeById(typeId);
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
      JsonSchemaValidationUtil.validate(config, schema);
    } else {
      log.warn(
          "No schema found for connector type ID: {}, skipping schema validation",
          connectorType.getId());
    }
  }
}
