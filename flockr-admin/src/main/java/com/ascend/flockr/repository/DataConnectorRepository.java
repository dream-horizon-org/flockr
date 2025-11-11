package com.ascend.flockr.repository;

import com.ascend.flockr.dto.model.dataconnectors.DataConnectorType;
import com.ascend.flockr.dto.model.dataconnectors.DataSinkDetails;
import com.ascend.flockr.dto.model.dataconnectors.DataSourceDetails;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface DataConnectorRepository {

  Single<List<DataConnectorType>> listConnectorTypes(String kind);

  Single<DataConnectorType> getConnectorTypeById(Long id);

  Single<Long> createDataSource(String name, Long typeId, String createdBy, String configJson);

  Single<Long> createDataSink(String name, Long typeId, String createdBy, String configJson);

  Single<List<DataSourceDetails>> listDataSources(int page, int pageSize);

  Single<List<DataSinkDetails>> listDataSinks(int page, int pageSize);
}
