package com.ascend.flockr.service;

import com.ascend.flockr.domain.dataconnectors.DataConnectorType;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import com.ascend.flockr.io.request.OnboardDataSinkRequest;
import com.ascend.flockr.io.request.OnboardDataSourceRequest;
import com.ascend.flockr.io.response.PaginatedResponse;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface DataConnectorService {
  Single<List<DataConnectorType>> listTypes(String kind);

  Single<DataSourceDetails> onboardSource(OnboardDataSourceRequest request, String createdBy);

  Single<DataSinkDetails> onboardSink(OnboardDataSinkRequest request, String createdBy);

  Single<PaginatedResponse<DataSourceDetails>> listSources(int page, int pageSize);

  Single<PaginatedResponse<DataSinkDetails>> listSinks(int page, int pageSize);
}
