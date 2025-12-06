package io.ascend.flockr.admin.repository.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.postgres.PostgresReaderClient;
import io.ascend.flockr.admin.client.postgres.PostgresWriterClient;
import io.ascend.flockr.admin.domain.dataconnectors.DataConnectorType;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link DataConnectorRepository} using PostgreSQL as the data store.
 *
 * <p>This implementation handles:
 *
 * <ul>
 *   <li>CRUD operations for connector types, data sources, and data sinks
 *   <li>Pagination for source and sink listings
 *   <li>Batch retrieval operations using dynamic SQL with IN clauses
 *   <li>Transaction management for data integrity
 * </ul>
 *
 * <p>All write operations are executed within database transactions to ensure atomicity.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DataConnectorRepositoryImpl implements DataConnectorRepository {

  private final PostgresReaderClient postgresReaderClient;
  private final PostgresWriterClient postgresWriterClient;

  private static final String SQL_LIST_TYPES =
      "SELECT id, kind, type, display_name, config_schema, is_active FROM data_connector_types WHERE kind = $1 AND is_active = TRUE ORDER BY display_name";

  private static final String SQL_GET_TYPE_BY_ID =
      "SELECT id, kind, type, display_name, config_schema, is_active FROM data_connector_types WHERE id = $1";

  private static final String SQL_CREATE_SOURCE =
      "INSERT INTO data_sources (name, type_id, config, created_by) VALUES ($1, $2, CAST($3 AS JSONB), $4) RETURNING id";

  private static final String SQL_CREATE_SINK =
      "INSERT INTO data_sinks (name, type_id, config, created_by) VALUES ($1, $2, CAST($3 AS JSONB), $4) RETURNING id";

  private static final String SQL_LIST_SOURCES =
      "SELECT s.id, s.name, s.type_id, t.type, s.config, s.status, s.created_by FROM data_sources s JOIN data_connector_types t ON s.type_id = t.id ORDER BY s.id DESC LIMIT $1 OFFSET $2";

  private static final String SQL_LIST_SINKS =
      "SELECT s.id, s.name, s.type_id, t.type, s.config, s.status, s.created_by FROM data_sinks s JOIN data_connector_types t ON s.type_id = t.id ORDER BY s.id DESC LIMIT $1 OFFSET $2";

  private static final String SQL_GET_SOURCES_BY_IDS =
      "SELECT s.id, s.name, s.type_id, t.type, s.config, s.status, s.created_by FROM data_sources s JOIN data_connector_types t ON s.type_id = t.id WHERE s.id IN (%s)";

  private static final String SQL_GET_SINKS_BY_IDS =
      "SELECT s.id, s.name, s.type_id, t.type, s.config, s.status, s.created_by FROM data_sinks s JOIN data_connector_types t ON s.type_id = t.id WHERE s.id IN (%s)";

  private static final String SQL_CREATE_CONNECTOR_TYPE =
      "INSERT INTO data_connector_types (kind, type, display_name, config_schema, is_active) VALUES ($1, $2, $3, $4::jsonb, TRUE) RETURNING id";

  @Override
  public Single<List<DataConnectorType>> listConnectorTypes(String kind) {
    return postgresReaderClient
        .fetchAll(SQL_LIST_TYPES, Tuple.of(kind), DataConnectorType::mapTypeRow)
        .doOnError(
            error ->
                log.error(
                    "Error listing connector types for kind: {}. Query: {}",
                    kind,
                    SQL_LIST_TYPES,
                    error));
  }

  @Override
  public Single<DataConnectorType> getConnectorTypeById(Long id) {
    return postgresReaderClient
        .fetchOne(SQL_GET_TYPE_BY_ID, Tuple.of(id), DataConnectorType::mapTypeRow)
        .doOnError(
            error ->
                log.error(
                    "Error getting connector type by id: {}. Query: {}",
                    id,
                    SQL_GET_TYPE_BY_ID,
                    error));
  }

  @Override
  public Single<Long> createDataSource(
      String name, Long typeId, String createdBy, JsonObject configJson) {
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .executeAndGenerateId(
                        conn, SQL_CREATE_SOURCE, Tuple.of(name, typeId, configJson, createdBy))
                    .toMaybe())
        .doOnError(
            error ->
                log.error(
                    "Error creating data source. name: {}, typeId: {}, createdBy: {}. Query: {}",
                    name,
                    typeId,
                    createdBy,
                    SQL_CREATE_SOURCE,
                    error))
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to create data source")))
        .toSingle();
  }

  @Override
  public Single<Long> createDataSink(
      String name, Long typeId, String createdBy, JsonObject configJson) {
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .executeAndGenerateId(
                        conn, SQL_CREATE_SINK, Tuple.of(name, typeId, configJson, createdBy))
                    .toMaybe())
        .doOnError(
            error ->
                log.error(
                    "Error creating data sink. name: {}, typeId: {}, createdBy: {}. Query: {}",
                    name,
                    typeId,
                    createdBy,
                    SQL_CREATE_SINK,
                    error))
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to create data sink")))
        .toSingle();
  }

  @Override
  public Single<List<DataSourceDetails>> listDataSources(int page, int pageSize) {
    int offset = page * pageSize;
    return postgresReaderClient
        .fetchAll(SQL_LIST_SOURCES, Tuple.of(pageSize, offset), DataSourceDetails::mapSourceRow)
        .doOnError(
            error ->
                log.error(
                    "Error listing data sources. page: {}, pageSize: {}, offset: {}. Query: {}",
                    page,
                    pageSize,
                    offset,
                    SQL_LIST_SOURCES,
                    error));
  }

  @Override
  public Single<List<DataSinkDetails>> listDataSinks(int page, int pageSize) {
    int offset = page * pageSize;
    return postgresReaderClient
        .fetchAll(SQL_LIST_SINKS, Tuple.of(pageSize, offset), DataSinkDetails::mapSinkRow)
        .doOnError(
            error ->
                log.error(
                    "Error listing data sinks. page: {}, pageSize: {}, offset: {}. Query: {}",
                    page,
                    pageSize,
                    offset,
                    SQL_LIST_SINKS,
                    error));
  }

  @Override
  public Single<List<DataSourceDetails>> getDataSourcesByIds(List<Long> sourceIds) {
    if (sourceIds == null || sourceIds.isEmpty()) {
      return Single.just(List.of());
    }

    String commaSeparatedIds =
        String.join(",", sourceIds.stream().map(String::valueOf).toArray(String[]::new));
    String query = String.format(SQL_GET_SOURCES_BY_IDS, commaSeparatedIds);

    return postgresReaderClient
        .fetchAll(query, DataSourceDetails::mapSourceRow)
        .doOnError(
            error ->
                log.error(
                    "Error getting data sources by ids: {}. Query: {}", sourceIds, query, error));
  }

  /**
   * Retrieves multiple data sinks by their identifiers.
   *
   * <p>This method builds a dynamic SQL query using the provided sink IDs. Returns an empty list if
   * the input list is null or empty.
   *
   * @param sinkIds the list of sink IDs to retrieve
   * @return a Single emitting a list of data sink details
   */
  @Override
  public Single<List<DataSinkDetails>> getDataSinksByIds(List<Long> sinkIds) {
    if (sinkIds == null || sinkIds.isEmpty()) {
      return Single.just(List.of());
    }

    String commaSeparatedIds =
        String.join(",", sinkIds.stream().map(String::valueOf).toArray(String[]::new));
    String query = String.format(SQL_GET_SINKS_BY_IDS, commaSeparatedIds);

    return postgresReaderClient
        .fetchAll(query, DataSinkDetails::mapSinkRow)
        .doOnError(
            error ->
                log.error("Error getting data sinks by ids: {}. Query: {}", sinkIds, query, error));
  }

  @Override
  public Single<Long> createConnectorType(
      String kind, String type, String displayName, String createdBy, JsonObject jsonObject) {
    log.info(
        "Creating connector type. kind: {}, type: {}, displayName: {}, createdBy: {}, configSchemaJson: {}",
        kind,
        type,
        displayName,
        createdBy,
        jsonObject);
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .executeAndGenerateId(
                        conn,
                        SQL_CREATE_CONNECTOR_TYPE,
                        Tuple.of(kind, type, displayName, jsonObject))
                    .toMaybe())
        .doOnError(
            error ->
                log.error(
                    "Error creating connector type. kind: {}, type: {}, displayName: {}, createdBy: {}. Query: {}",
                    kind,
                    type,
                    displayName,
                    createdBy,
                    SQL_CREATE_CONNECTOR_TYPE,
                    error))
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to create connector type")))
        .toSingle();
  }
}
