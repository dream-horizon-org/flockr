package com.ascend.flockr.service.impl;

import com.ascend.flockr.domain.dataconnectors.DataConnectorType;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import com.ascend.flockr.io.request.OnboardDataSinkRequest;
import com.ascend.flockr.io.request.OnboardDataSourceRequest;
import com.ascend.flockr.io.response.PaginatedResponse;
import com.ascend.flockr.repository.DataConnectorRepository;
import com.ascend.flockr.service.DataConnectorService;
import com.ascend.flockr.util.ConfigValidatorRegistry;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DataConnectorServiceImpl implements DataConnectorService {

  private final DataConnectorRepository repository;
  private final ConfigValidatorRegistry configValidatorRegistry;

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
              configValidatorRegistry.validate(request.getConfig(), type.getType(), type.getKind());
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
              configValidatorRegistry.validate(request.getConfig(), type.getType(), type.getKind());
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
}
