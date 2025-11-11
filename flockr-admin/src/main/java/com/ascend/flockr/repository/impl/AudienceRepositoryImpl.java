package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.mysql.MySQLReaderClient;
import com.ascend.flockr.client.mysql.MySQLWriterClient;
import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.repository.AudienceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AudienceRepositoryImpl implements AudienceRepository {
  private final MySQLReaderClient mySQLReaderClient;
  private final MySQLWriterClient mySQLWriterClient;
  private final ObjectMapper objectMapper;

  private static final String SQL_CREATE_AUDIENCE =
      "INSERT INTO audience (tenant_id, project_id, name, description, sinks, custom_audience_config, type, expiry_date) "
          + "VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), ?, FROM_UNIXTIME(?))";

  private static final String SQL_LINK_SINKS =
      "INSERT IGNORE INTO audience_sinks (audience_id, sink_id) VALUES (?, ?)";

  private static final String SQL_GET_AUDIENCE_BY_ID =
      "SELECT id, tenant_id, project_id, name, description, client, created_by, created_at, updated_at, "
          + "last_audience_updated_at, user_count, custom_audience_config, type, verified, rules_count "
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

    return mySQLWriterClient
        .executeWithTransaction(
            conn ->
                mySQLWriterClient.executeAndGenerateId(conn, SQL_CREATE_AUDIENCE, params).toMaybe())
        .toSingle();
  }

  @Override
  public Single<Long> addOwner(String email, Long audienceId) {
    return null;
  }

  @Override
  public Single<AudienceMeta> getAudienceById(Long id) {
    return null;
  }
}
