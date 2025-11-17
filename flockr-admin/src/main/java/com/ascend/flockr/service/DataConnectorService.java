package com.ascend.flockr.service;

import com.ascend.flockr.domain.dataconnectors.DataConnectorType;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import com.ascend.flockr.io.request.OnboardConnectorTypeRequest;
import com.ascend.flockr.io.request.OnboardDataSinkRequest;
import com.ascend.flockr.io.request.OnboardDataSourceRequest;
import com.ascend.flockr.io.response.PaginatedResponse;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

/**
 * Service interface for managing data connectors (sources and sinks).
 *
 * <p>This service provides operations for:
 * <ul>
 *   <li>Onboarding connector types, data sources, and data sinks</li>
 *   <li>Listing available connector types</li>
 *   <li>Listing data sources and sinks with pagination</li>
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
public interface DataConnectorService {
  /**
   * Lists available connector types filtered by kind (SOURCE or SINK).
   *
   * @param kind the kind of connector to list ("SOURCE" or "SINK")
   * @return a Single emitting a list of connector types
   */
  Single<List<DataConnectorType>> listTypes(String kind);

  /**
   * Onboards a new data source connector.
   *
   * @param request the request containing data source configuration
   * @param createdBy the username of the user creating the data source
   * @return a Single emitting the created data source details
   */
  Single<DataSourceDetails> onboardSource(OnboardDataSourceRequest request, String createdBy);

  /**
   * Onboards a new data sink connector.
   *
   * @param request the request containing data sink configuration
   * @param createdBy the username of the user creating the data sink
   * @return a Single emitting the created data sink details
   */
  Single<DataSinkDetails> onboardSink(OnboardDataSinkRequest request, String createdBy);

  /**
   * Lists data sources with pagination.
   *
   * @param page the page number (0-indexed)
   * @param pageSize the number of results per page
   * @return a Single emitting a paginated response containing data source details
   */
  Single<PaginatedResponse<DataSourceDetails>> listSources(int page, int pageSize);

  /**
   * Lists data sinks with pagination.
   *
   * @param page the page number (0-indexed)
   * @param pageSize the number of results per page
   * @return a Single emitting a paginated response containing data sink details
   */
  Single<PaginatedResponse<DataSinkDetails>> listSinks(int page, int pageSize);

  /**
   * Onboards a new connector type.
   *
   * @param request the request containing connector type configuration
   * @param createdBy the username of the user creating the connector type
   * @return a Single emitting the created connector type
   */
  Single<DataConnectorType> onboardConnectorType(
      OnboardConnectorTypeRequest request, String createdBy);
}
