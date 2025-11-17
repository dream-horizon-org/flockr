package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.client.postgres.PostgresWriterClient;
import com.ascend.flockr.domain.dataconnectors.DataConnectorType;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import com.ascend.flockr.repository.DataConnectorRepository;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DataConnectorRepositoryImpl implements DataConnectorRepository {

  private final PostgresReaderClient postgresReaderClient;
  private final PostgresWriterClient postgresWriterClient;

  private static final String SQL_LIST_TYPES =
      "SELECT id, kind, type, display_name, is_active FROM data_connector_types WHERE kind = ? AND is_active = TRUE ORDER BY display_name";

  private static final String SQL_GET_TYPE_BY_ID =
      "SELECT id, kind, type, display_name, is_active FROM data_connector_types WHERE id = ?";

  private static final String SQL_CREATE_SOURCE =
      "INSERT INTO data_sources (name, type_id, config, created_by) VALUES (?, ?, CAST(? AS JSONB), ?) RETURNING id";

  private static final String SQL_CREATE_SINK =
      "INSERT INTO data_sinks (name, type_id, config, created_by) VALUES (?, ?, CAST(? AS JSONB), ?) RETURNING id";

  private static final String SQL_LIST_SOURCES =
      "SELECT s.id, s.name, s.type_id, t.type, s.config, s.status, s.created_by FROM data_sources s JOIN data_connector_types t ON s.type_id = t.id ORDER BY s.id DESC LIMIT ? OFFSET ?";

  private static final String SQL_LIST_SINKS =
      "SELECT s.id, s.name, s.type_id, t.type, s.config, s.status, s.created_by FROM data_sinks s JOIN data_connector_types t ON s.type_id = t.id ORDER BY s.id DESC LIMIT ? OFFSET ?";

  private static final String SQL_GET_SOURCES_BY_IDS =
      "SELECT s.id, s.name, s.type_id, t.type, s.config, s.status, s.created_by FROM data_sources s JOIN data_connector_types t ON s.type_id = t.id WHERE s.id IN (%s)";

  private static final String SQL_GET_SINKS_BY_IDS =
      "SELECT s.id, s.name, s.type_id, t.type, s.config, s.status, s.created_by FROM data_sinks s JOIN data_connector_types t ON s.type_id = t.id WHERE s.id IN (%s)";

  private static final String SQL_CREATE_CONNECTOR_TYPE =
      "INSERT INTO data_connector_types (kind, type, display_name, config_schema, is_active) VALUES (?, ?, ?, CAST(? AS JSONB), TRUE) RETURNING id";

  @Override
  public Single<List<DataConnectorType>> listConnectorTypes(String kind) {
    return postgresReaderClient.fetchAll(
        SQL_LIST_TYPES, Tuple.of(kind), DataConnectorType::mapTypeRow);
  }

  @Override
  public Single<DataConnectorType> getConnectorTypeById(Long id) {
    return postgresReaderClient.fetchOne(
        SQL_GET_TYPE_BY_ID, Tuple.of(id), DataConnectorType::mapTypeRow);
  }

  @Override
  public Single<Long> createDataSource(
      String name, Long typeId, String createdBy, String configJson) {
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .executeAndGenerateId(
                        conn, SQL_CREATE_SOURCE, Tuple.of(name, typeId, configJson, createdBy))
                    .toMaybe())
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to create data source")))
        .toSingle();
  }

  @Override
  public Single<Long> createDataSink(
      String name, Long typeId, String createdBy, String configJson) {
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .executeAndGenerateId(
                        conn, SQL_CREATE_SINK, Tuple.of(name, typeId, configJson, createdBy))
                    .toMaybe())
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to create data sink")))
        .toSingle();
  }

  @Override
  public Single<List<DataSourceDetails>> listDataSources(int page, int pageSize) {
    int offset = page * pageSize;
    return postgresReaderClient.fetchAll(
        SQL_LIST_SOURCES, Tuple.of(pageSize, offset), DataSourceDetails::mapSourceRow);
  }

  @Override
  public Single<List<DataSinkDetails>> listDataSinks(int page, int pageSize) {
    int offset = page * pageSize;
    return postgresReaderClient.fetchAll(
        SQL_LIST_SINKS, Tuple.of(pageSize, offset), DataSinkDetails::mapSinkRow);
  }

  @Override
  public Single<List<DataSourceDetails>> getDataSourcesByIds(List<Long> sourceIds) {
    if (sourceIds == null || sourceIds.isEmpty()) {
      return Single.just(List.of());
    }

    // Build placeholders for IN clause
    String placeholders = String.join(",", sourceIds.stream().map(id -> "?").toList());
    String query = String.format(SQL_GET_SOURCES_BY_IDS, placeholders);

    // Build tuple with all IDs
    Tuple tuple = Tuple.tuple();
    for (Long sourceId : sourceIds) {
      tuple.addValue(sourceId);
    }

    return postgresReaderClient.fetchAll(query, tuple, DataSourceDetails::mapSourceRow);
  }

  /**
   * @param sinkIds
   * @return
   */
  @Override
  public Single<List<DataSinkDetails>> getDataSinksByIds(List<Long> sinkIds) {
    if (sinkIds == null || sinkIds.isEmpty()) {
      return Single.just(List.of());
    }

    // Build placeholders for IN clause
    String placeholders = String.join(",", sinkIds.stream().map(id -> "?").toList());
    String query = String.format(SQL_GET_SINKS_BY_IDS, placeholders);

    // Build tuple with all IDs
    Tuple tuple = Tuple.tuple();
    for (Long sinkId : sinkIds) {
      tuple.addValue(sinkId);
    }

    return postgresReaderClient.fetchAll(query, tuple, DataSinkDetails::mapSinkRow);
  }

  @Override
  public Single<Long> createConnectorType(
      String kind, String type, String displayName, String createdBy, String configSchemaJson) {
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .executeAndGenerateId(
                        conn,
                        SQL_CREATE_CONNECTOR_TYPE,
                        Tuple.of(kind, type, displayName, configSchemaJson))
                    .toMaybe())
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to create connector type")))
        .toSingle();
  }
}
