package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.client.postgres.PostgresWriterClient;
import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.repository.AudienceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AudienceRepositoryImpl implements AudienceRepository {
  private final PostgresReaderClient postgresReaderClient;
  private final PostgresWriterClient postgresWriterClient;
  private final ObjectMapper objectMapper;

  private static final String SQL_CREATE_AUDIENCE =
      "INSERT INTO audience (tenant_id, project_id, name, description, sinks, custom_audience_config, type, expiry_date) "
          + "VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), ?, FROM_UNIXTIME(?))";

  private static final String SQL_GET_AUDIENCE_BY_ID =
      "SELECT id, tenant_id, project_id, name, description, "
          + "UNIX_TIMESTAMP(created_at) AS created_at, "
          + "UNIX_TIMESTAMP(updated_at) AS updated_at, "
          + "UNIX_TIMESTAMP(last_audience_updated_at) AS last_audience_updated_at, "
          + "user_count, custom_audience_config, type, verified, rules_count, "
          + "UNIX_TIMESTAMP(expiry_date) AS expiry_date, sinks "
          + "FROM audience WHERE id = ?";

  @Override
  public Single<Long> createAudience(AudienceMeta audienceMeta) {
    Tuple params =
        Tuple.tuple()
            .addValue(audienceMeta.getTenantId())
            .addValue(audienceMeta.getProjectId())
            .addValue(audienceMeta.getCreatedAt())
            .addValue(audienceMeta.getDescription())
            .addValue(audienceMeta.getSinks())
            .addValue(audienceMeta.getCustomAudienceConfig())
            .addValue(audienceMeta.getType())
            .addValue(audienceMeta.getExpireDate());

    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .executeAndGenerateId(conn, SQL_CREATE_AUDIENCE, params)
                    .toMaybe())
        .toSingle();
  }

  @Override
  public Single<AudienceMeta> getAudienceById(Long id) {
    return postgresReaderClient.fetchOne(
        SQL_GET_AUDIENCE_BY_ID,
        Tuple.of(id),
        row -> {
          AudienceMeta.AudienceMetaBuilder builder =
              AudienceMeta.builder()
                  .audienceId(row.getLong("id"))
                  .tenantId(row.getString("tenant_id"))
                  .projectId(row.getString("project_id"))
                  .name(row.getString("name"))
                  .description(row.getString("description"))
                  .type(row.getString("type"))
                  .verified(row.getBoolean("verified"))
                  .userCount(row.getLong("user_count"))
                  .expireDate(row.getLong("expiry_date"))
                  .lastAudienceUpdatedAt(row.getLong("last_audience_updated_at"))
                  .createdAt(row.getLong("created_at"))
                  .updatedAt(row.getLong("updated_at"));

          // Map JSON fields
          String configJson = row.getString("custom_audience_config");
          if (configJson != null) {
            try {
              JsonNode configNode = objectMapper.readTree(configJson);
              builder.customAudienceConfig(configNode);
            } catch (Exception e) {
              throw new RuntimeException(
                  "Failed to deserialize custom_audience_config for audience id " + id, e);
            }
          }

          String sinksJson = row.getString("sinks");
          if (sinksJson != null && !sinksJson.isBlank()) {
            try {
              List<Long> sinks = objectMapper.readValue(sinksJson, new TypeReference<>() {});
              builder.sinks(sinks);
            } catch (Exception e) {
              throw new RuntimeException(
                  "Failed to deserialize sinks for audience id " + id + ": " + sinksJson, e);
            }
          }

          return builder.build();
        });
  }
}
