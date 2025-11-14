package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.mysql.MySQLReaderClient;
import com.ascend.flockr.client.mysql.MySQLWriterClient;
import com.ascend.flockr.domain.dataconnectors.DataConnectorType;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import com.ascend.flockr.repository.DataConnectorRepository;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DataConnectorRepositoryImpl implements DataConnectorRepository {

  private final MySQLReaderClient mySQLReaderClient;
  private final MySQLWriterClient mySQLWriterClient;

  private static final String SQL_LIST_TYPES =
      "SELECT id, kind, type, display_name, is_active FROM data_connector_types WHERE kind = ? AND is_active = 1 ORDER BY display_name";

  private static final String SQL_GET_TYPE_BY_ID =
      "SELECT id, kind, type, display_name, is_active FROM data_connector_types WHERE id = ?";

  private static final String SQL_CREATE_SOURCE =
      "INSERT INTO data_sources (name, type_id, config, created_by) VALUES (?, ?, CAST(? AS JSON), ?)";

  private static final String SQL_CREATE_SINK =
      "INSERT INTO data_sinks (name, type_id, config, created_by) VALUES (?, ?, CAST(? AS JSON), ?)";

  private static final String SQL_LIST_SOURCES =
      "SELECT s.id, s.name, s.type_id, t.type, s.config, s.status, s.created_by FROM data_sources s JOIN data_connector_types t ON s.type_id = t.id ORDER BY s.id DESC LIMIT ? OFFSET ?";

  private static final String SQL_LIST_SINKS =
      "SELECT s.id, s.name, s.type_id, t.type, s.config, s.status, s.created_by FROM data_sinks s JOIN data_connector_types t ON s.type_id = t.id ORDER BY s.id DESC LIMIT ? OFFSET ?";

  private static final String SQL_GET_SOURCES_BY_IDS =
      "SELECT s.id, s.name, s.type_id, t.type, s.config, s.status, s.created_by FROM data_sources s JOIN data_connector_types t ON s.type_id = t.id WHERE s.id IN (%s)";

  @Override
  public Single<List<DataConnectorType>> listConnectorTypes(String kind) {
    return mySQLReaderClient.fetchAll(
        SQL_LIST_TYPES, Tuple.of(kind), DataConnectorRepositoryImpl::mapTypeRow);
  }

  @Override
  public Single<DataConnectorType> getConnectorTypeById(Long id) {
    return mySQLReaderClient.fetchOne(
        SQL_GET_TYPE_BY_ID, Tuple.of(id), DataConnectorRepositoryImpl::mapTypeRow);
  }

  @Override
  public Single<Long> createDataSource(
      String name, Long typeId, String createdBy, String configJson) {
    return mySQLWriterClient
        .executeWithTransaction(
            conn ->
                mySQLWriterClient
                    .executeAndGenerateId(
                        conn, SQL_CREATE_SOURCE, Tuple.of(name, typeId, configJson, createdBy))
                    .toMaybe())
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to create data source")))
        .toSingle();
  }

  @Override
  public Single<Long> createDataSink(
      String name, Long typeId, String createdBy, String configJson) {
    return mySQLWriterClient
        .executeWithTransaction(
            conn ->
                mySQLWriterClient
                    .executeAndGenerateId(
                        conn, SQL_CREATE_SINK, Tuple.of(name, typeId, configJson, createdBy))
                    .toMaybe())
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to create data sink")))
        .toSingle();
  }

  @Override
  public Single<List<DataSourceDetails>> listDataSources(int page, int pageSize) {
    int offset = page * pageSize;
    return mySQLReaderClient.fetchAll(
        SQL_LIST_SOURCES, Tuple.of(pageSize, offset), DataConnectorRepositoryImpl::mapSourceRow);
  }

  @Override
  public Single<List<DataSinkDetails>> listDataSinks(int page, int pageSize) {
    int offset = page * pageSize;
    return mySQLReaderClient.fetchAll(
        SQL_LIST_SINKS, Tuple.of(pageSize, offset), DataConnectorRepositoryImpl::mapSinkRow);
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

    return mySQLReaderClient.fetchAll(query, tuple, DataConnectorRepositoryImpl::mapSourceRow);
  }

  private static DataConnectorType mapTypeRow(Row row) {
    return DataConnectorType.builder()
        .id(row.getLong("id"))
        .kind(row.getString("kind"))
        .type(row.getString("type"))
        .displayName(row.getString("display_name"))
        .active(row.getBoolean("is_active"))
        .build();
  }

  private static DataSourceDetails mapSourceRow(Row row) {
    return DataSourceDetails.builder()
        .id(row.getLong("id"))
        .name(row.getString("name"))
        .typeId(row.getLong("type_id"))
        .type(row.getString("type"))
        .config(row.getJsonObject("config"))
        .status(row.getString("status"))
        .createdBy(row.getString("created_by"))
        .build();
  }

  private static DataSinkDetails mapSinkRow(Row row) {
    return DataSinkDetails.builder()
        .id(row.getLong("id"))
        .name(row.getString("name"))
        .typeId(row.getLong("type_id"))
        .type(row.getString("type"))
        .config(row.getJsonObject("config"))
        .status(row.getString("status"))
        .createdBy(row.getString("created_by"))
        .build();
  }
}
