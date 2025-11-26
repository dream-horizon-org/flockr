package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.client.postgres.PostgresWriterClient;
import com.ascend.flockr.domain.audienceOwner.AudienceOwner;
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

    private static final String FIND_OWNERS_BY_AUDIENCE_ID =
            "SELECT id, audience_id AS audience_id, name, status, "
                    + "EXTRACT(EPOCH FROM created_at)::BIGINT AS created_at "
                    + "FROM audience_owners WHERE audience_id = $1 AND tenant_id = $2 AND project_id = $3 ORDER BY created_at DESC";

  private static final String INSERT_AUDIENCE_OWNER =
      "INSERT INTO audience_owners (audience_id, tenant_id, project_id, name, status) "
          + "SELECT $1, a.tenant_id, a.project_id, $4, 'ACTIVE' FROM audiences a WHERE a.id = $1 AND a.tenant_id = $2 AND a.project_id = $3";

  private static final String REMOVE_AUDIENCE_OWNER =
      "UPDATE audience_owners SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP "
          + "WHERE audience_id = $1 AND tenant_id = $2 AND project_id = $3 AND name = $4 AND status = 'ACTIVE'";

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

  private static AudienceOwner mapOwnerRow(Row row) {
    AudienceOwner audienceOwner = new AudienceOwner();
    audienceOwner.setId(row.getLong("id"));
    audienceOwner.setAudienceId(row.getLong("audience_id"));
    audienceOwner.setOwner(row.getString("name"));
    String ownerStatus = row.getString("status");
    audienceOwner.setIsRemoved(ownerStatus != null && !"ACTIVE".equalsIgnoreCase(ownerStatus));
    audienceOwner.setRemovedBy(null);
    audienceOwner.setAddedBy(null);
    audienceOwner.setCreatedAt(row.getLong("created_at"));
    return audienceOwner;
  }
}
