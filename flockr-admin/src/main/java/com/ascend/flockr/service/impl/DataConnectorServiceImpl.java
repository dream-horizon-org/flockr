package com.ascend.flockr.service.impl;

import com.ascend.flockr.domain.dataconnectors.DataConnectorType;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import com.ascend.flockr.io.request.OnboardConnectorTypeRequest;
import com.ascend.flockr.io.request.OnboardDataSinkRequest;
import com.ascend.flockr.io.request.OnboardDataSourceRequest;
import com.ascend.flockr.io.response.PaginatedResponse;
import com.ascend.flockr.repository.DataConnectorRepository;
import com.ascend.flockr.service.DataConnectorService;
import com.ascend.flockr.util.json.JsonSchemaValidationUtil;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DataConnectorServiceImpl implements DataConnectorService {

  private final DataConnectorRepository repository;
  private final JsonSchemaValidationUtil schemaValidator;

  @Override
  public Single<List<DataConnectorType>> listTypes(String kind) {
    return repository.listConnectorTypes(kind);
  }

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
                        request.getName(),
                        request.getTypeId(),
                        createdBy,
                        request.getConfig().encode())
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
                        request.getName(),
                        request.getTypeId(),
                        createdBy,
                        request.getConfig().encode())
                    .map(
                        id ->
                            DataSinkDetails.builder()
                                .id(id)
                                .name(request.getName())
                                .typeId(request.getTypeId())
                                .config(request.getConfig())
                                .status("ACTIVE")
                                .createdBy(createdBy)
                                .build()));
  }

  @Override
  public Single<PaginatedResponse<DataSourceDetails>> listSources(int page, int pageSize) {
    return repository
        .listDataSources(page, pageSize)
        .map(
            list ->
                new PaginatedResponse<>(
                    new PaginatedResponse.PageInfo(page, pageSize, list.size() == pageSize), list));
  }

  @Override
  public Single<PaginatedResponse<DataSinkDetails>> listSinks(int page, int pageSize) {
    return repository
        .listDataSinks(page, pageSize)
        .map(
            list ->
                new PaginatedResponse<>(
                    new PaginatedResponse.PageInfo(page, pageSize, list.size() == pageSize), list));
  }

  @Override
  public Single<DataConnectorType> onboardConnectorType(
      OnboardConnectorTypeRequest request, String createdBy) {
    // Validate kind is either SOURCE or SINK
    String kind = request.getKind().toUpperCase();
    if (!"SOURCE".equals(kind) && !"SINK".equals(kind)) {
      return Single.error(new IllegalArgumentException("Kind must be either 'SOURCE' or 'SINK'"));
    }

    String configSchemaJson = request.getConfigSchema().encode();

    return repository
        .createConnectorType(
            kind, request.getType(), request.getDisplayName(), createdBy, configSchemaJson)
        .flatMap(repository::getConnectorTypeById);
  }

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
      schemaValidator.validate(config, schema);
    } else {
      log.warn(
          "No schema found for connector type ID: {}, skipping schema validation",
          connectorType.getId());
    }
  }
}
