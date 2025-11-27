package io.ascend.flockr.admin.repository;

import io.ascend.flockr.admin.domain.dataconnectors.DataConnectorType;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * Repository interface for accessing data connector information from the database.
 *
 * <p>This repository provides methods for:
 *
 * <ul>
 *   <li>Managing connector types (SOURCE and SINK)
 *   <li>Creating and retrieving data sources
 *   <li>Creating and retrieving data sinks
 *   <li>Batch retrieval operations for sources and sinks
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
public interface DataConnectorRepository {

  /**
   * Lists all connector types filtered by kind.
   *
   * @param kind the kind of connector ("SOURCE" or "SINK")
   * @return a Single emitting a list of active connector types
   */
  Single<List<DataConnectorType>> listConnectorTypes(String kind);

  /**
   * Retrieves a connector type by its unique identifier.
   *
   * @param id the connector type identifier
   * @return a Single emitting the connector type details
   */
  Single<DataConnectorType> getConnectorTypeById(Long id);

  /**
   * Creates a new data source in the database.
   *
   * @param name the name of the data source
   * @param typeId the ID of the connector type
   * @param createdBy the username of the creator
   * @param configJson the JSON configuration as a string
   * @return a Single emitting the generated data source ID
   */
  Single<Long> createDataSource(String name, Long typeId, String createdBy, JsonObject configJson);

  /**
   * Creates a new data sink in the database.
   *
   * @param name the name of the data sink
   * @param typeId the ID of the connector type
   * @param createdBy the username of the creator
   * @param configJson the JSON configuration as a string
   * @return a Single emitting the generated data sink ID
   */
  Single<Long> createDataSink(String name, Long typeId, String createdBy, JsonObject configJson);

  /**
   * Lists data sources with pagination.
   *
   * @param page the page number (0-indexed)
   * @param pageSize the number of results per page
   * @return a Single emitting a list of data source details
   */
  Single<List<DataSourceDetails>> listDataSources(int page, int pageSize);

  /**
   * Lists data sinks with pagination.
   *
   * @param page the page number (0-indexed)
   * @param pageSize the number of results per page
   * @return a Single emitting a list of data sink details
   */
  Single<List<DataSinkDetails>> listDataSinks(int page, int pageSize);

  /**
   * Retrieves multiple data sources by their identifiers.
   *
   * @param sourceIds the list of source IDs to retrieve
   * @return a Single emitting a list of data source details
   */
  Single<List<DataSourceDetails>> getDataSourcesByIds(List<Long> sourceIds);

  /**
   * Retrieves multiple data sinks by their identifiers.
   *
   * @param sinkIds the list of sink IDs to retrieve
   * @return a Single emitting a list of data sink details
   */
  Single<List<DataSinkDetails>> getDataSinksByIds(List<Long> sinkIds);

  /**
   * Creates a new connector type in the database.
   *
   * @param kind the kind of connector ("SOURCE" or "SINK")
   * @param type the type identifier for the connector
   * @param displayName the human-readable display name
   * @param createdBy the username of the creator
   * @param configSchemaJson the JSON schema for validating connector configurations
   * @return a Single emitting the generated connector type ID
   */
  Single<Long> createConnectorType(
      String kind, String type, String displayName, String createdBy, JsonObject configSchemaJson);
}
