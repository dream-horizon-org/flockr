package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.client.postgres.PostgresWriterClient;
import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.domain.cohort.AudienceOwner;
import com.ascend.flockr.repository.AudienceOwnerRepository;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AudienceOwnerRepositoryImpl implements AudienceOwnerRepository {

  private final PostgresReaderClient postgresReaderClient;
  private final PostgresWriterClient postgresWriterClient;

    private static final String FIND_AUDIENCE_BY_ID =
            "SELECT id, name, description, verified, " +
                    "EXTRACT(EPOCH FROM created_at)::BIGINT AS created_at, " +
                    "EXTRACT(EPOCH FROM updated_at)::BIGINT AS updated_at, " +
                    "EXTRACT(EPOCH FROM expire_date)::BIGINT AS expire_date, " +
                    "user_count, created_by " +
                    "FROM audiences WHERE id = $1 AND tenant_id = $2 AND project_id = $3";

    private static final String FIND_OWNERS_BY_AUDIENCE_ID =
            "SELECT id, audience_id AS cohort_id, name, status, "
                    + "EXTRACT(EPOCH FROM created_at)::BIGINT AS created_at "
                    + "FROM audience_owners WHERE audience_id = $1 AND tenant_id = $2 AND project_id = $3 ORDER BY created_at DESC";

  private static final String INSERT_AUDIENCE_OWNER =
      "INSERT INTO audience_owners (audience_id, tenant_id, project_id, name, status) "
          + "SELECT $1, a.tenant_id, a.project_id, $4, 'ACTIVE' FROM audiences a WHERE a.id = $1 AND a.tenant_id = $2 AND a.project_id = $3";

  private static final String REMOVE_AUDIENCE_OWNER =
      "UPDATE audience_owners SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP "
          + "WHERE audience_id = $1 AND tenant_id = $2 AND project_id = $3 AND name = $4 AND status = 'ACTIVE'";

  @Override
  public Single<AudienceMeta> findById(String tenantId, String projectId, Long audienceId) {
    return postgresReaderClient.fetchOne(FIND_AUDIENCE_BY_ID, Tuple.of(audienceId, tenantId, projectId), AudienceOwnerRepositoryImpl::mapAudienceRow);
  }

  @Override
    public Single<List<AudienceOwner>> findOwners(String tenantId, String projectId, Long audienceId) {
        return postgresReaderClient.fetchAll(FIND_OWNERS_BY_AUDIENCE_ID, Tuple.of(audienceId, tenantId, projectId), AudienceOwnerRepositoryImpl::mapOwnerRow);
  }

  @Override
  public Single<Boolean> addOwner(String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail) {
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .execute(conn, INSERT_AUDIENCE_OWNER, Tuple.of(audienceId, tenantId, projectId, ownerEmail))
                    .toMaybe())
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to add owner")))
        .toSingle();
  }

  @Override
  public Single<Boolean> removeOwner(String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail) {
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .execute(conn, REMOVE_AUDIENCE_OWNER, Tuple.of(audienceId, tenantId, projectId, ownerEmail))
                    .toMaybe())
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to remove owner")))
        .toSingle();
  }

  private static AudienceMeta mapAudienceRow(Row row) {
    AudienceMeta m = new AudienceMeta();
    m.setAudienceId(row.getLong("id"));
    m.setName(row.getString("name"));
    m.setDescription(row.getString("description"));
    m.setVerified(row.getBoolean("verified"));
    m.setCreatedAt(row.getLong("created_at"));
    m.setUpdatedAt(row.getLong("updated_at"));
    m.setExpireDate(row.getLong("expire_date"));
    m.setUserCount(row.getLong("user_count"));
    m.setCreatedBy(row.getString("created_by"));
    return m;
  }

  private static AudienceOwner mapOwnerRow(Row row) {
    AudienceOwner o = new AudienceOwner();
    o.setId(row.getLong("id"));
    o.setAudienceId(row.getLong("cohort_id"));
    o.setOwner(row.getString("name"));
    String status = row.getString("status");
    o.setIsRemoved(status != null && !"ACTIVE".equalsIgnoreCase(status));
    o.setRemovedBy(null);
    o.setAddedBy(null);
    o.setCreatedAt(row.getLong("created_at"));
    return o;
  }
}
