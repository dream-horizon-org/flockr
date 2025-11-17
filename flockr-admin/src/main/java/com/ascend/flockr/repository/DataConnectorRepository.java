package com.ascend.flockr.repository;

import com.ascend.flockr.domain.dataconnectors.DataConnectorType;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface DataConnectorRepository {

  Single<List<DataConnectorType>> listConnectorTypes(String kind);

  Single<DataConnectorType> getConnectorTypeById(Long id);

  Single<Long> createDataSource(String name, Long typeId, String createdBy, String configJson);

  Single<Long> createDataSink(String name, Long typeId, String createdBy, String configJson);

  Single<List<DataSourceDetails>> listDataSources(int page, int pageSize);

  Single<List<DataSinkDetails>> listDataSinks(int page, int pageSize);

  Single<List<DataSourceDetails>> getDataSourcesByIds(List<Long> sourceIds);

  Single<List<DataSinkDetails>> getDataSinksByIds(List<Long> sinkIds);

  Single<Long> createConnectorType(
      String kind, String type, String displayName, String createdBy, String configSchemaJson);
}
