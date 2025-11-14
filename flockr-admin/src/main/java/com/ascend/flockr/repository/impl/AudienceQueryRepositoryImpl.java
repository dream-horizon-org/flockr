package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.io.response.AudienceDetailsResponse;
import com.ascend.flockr.io.response.AudienceDetailsResponse.RuleDetails;
import com.ascend.flockr.repository.AudienceQueryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AudienceQueryRepositoryImpl implements AudienceQueryRepository {

  private final PostgresReaderClient postgresReaderClient;
  private final ObjectMapper objectMapper;

  // Main query to fetch audience basic details including sinks JSON array
  private static final String SQL_GET_AUDIENCE =
      "SELECT id, tenant_id, project_id, name, description, type, custom_audience_config, "
          + "verified, user_count, rules_count, expiry_date, last_audience_updated_at, "
          + "sinks, created_at, updated_at "
          + "FROM audience WHERE id = ?";

  // Query to fetch sink metadata by IDs (using IN clause for batch fetch)
  private static final String SQL_GET_SINKS_BY_IDS =
      "SELECT ds.id, ds.name, ds.type_id, dct.type, ds.config, ds.status, ds.created_by "
          + "FROM data_sinks ds "
          + "JOIN data_connector_types dct ON ds.type_id = dct.id "
          + "WHERE ds.id IN (%s)";

  // Query to fetch all rules for an audience
  private static final String SQL_GET_AUDIENCE_RULES =
      "SELECT id, name, description, start_time, end_time, rule_action, rule_type, status, "
          + "configuration, created_by, updated_by, created_at, updated_at "
          + "FROM rules WHERE audience_id = ? ORDER BY created_at DESC";

  // Query to fetch source details by IDs
  private static final String SQL_GET_SOURCES_BY_IDS =
      "SELECT src.id, src.name, dct.type, src.status, src.created_by "
          + "FROM data_sources src "
          + "JOIN data_connector_types dct ON src.type_id = dct.id "
          + "WHERE src.id IN (%s)";

  @Override
  public Single<AudienceDetailsResponse> getAudienceDetails(Long audienceId) {
    // Step 1: Fetch audience basic details with sink IDs
    return getAudienceWithSinkIds(audienceId)
        .flatMap(
            result -> {
              List<Long> sinkIds = result.getSinks();
              // Step 2: Fetch sink metadata and rules in parallel
              Single<List<DataSinkDetails>> sinksSingle = getSinksByIds(sinkIds);
              Single<List<RuleDetails>> rulesSingle = getAudienceRulesWithSources(audienceId);

              // Combine all data
              return Single.zip(
                  sinksSingle,
                  rulesSingle,
                  (sinks, rules) -> {
                    audienceResponse.setSinks(sinks);
                    audienceResponse.setRules(rules);
                    return audienceResponse;
                  });
            });
  }

  /** Fetch sink metadata by IDs using single query with join */
  private Single<List<DataSinkDetails>> getSinksByIds(List<Long> sinkIds) {
    // Build query with IN clause
    String placeholders = String.join(",", sinkIds.stream().map(id -> "?").toList());
    String query = String.format(SQL_GET_SINKS_BY_IDS, placeholders);

    // Create tuple with all sink IDs
    Tuple tuple = Tuple.tuple();
    sinkIds.forEach(tuple::addLong);

    return postgresReaderClient
        .fetchAll(query, tuple, this::mapSinkRow)
        .doOnError(error -> log.error("Error fetching sinks by IDs: {}", error.getMessage()))
        .onErrorReturn(error -> new ArrayList<>()); // Return empty list on error
  }

  /** Map database row to DataSinkDetails */
  private DataSinkDetails mapSinkRow(Row row) {
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

  /** Convert LocalDateTime to epoch millis */
  private Long toEpochMillis(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.toInstant(ZoneOffset.UTC).toEpochMilli() : null;
  }
}
