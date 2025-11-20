package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.client.postgres.PostgresWriterClient;
import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.domain.cohort.CohortOwner;
import com.ascend.flockr.repository.CohortRepository;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class CohortRepositoryImpl implements CohortRepository {

  private final PostgresReaderClient postgresReaderClient;
  private final PostgresWriterClient postgresWriterClient;

    private static final String FIND_COHORT_BY_ID =
            "SELECT id, name, description, verified, "
                    + "EXTRACT(EPOCH FROM created_at)::BIGINT AS created_at, "
                    + "EXTRACT(EPOCH FROM updated_at)::BIGINT AS updated_at, "
                    + "EXTRACT(EPOCH FROM expire_date)::BIGINT AS expire_date, "
                    + "user_count, created_by "
                    + "FROM audiences WHERE id = ?";

    private static final String FIND_OWNERS_BY_COHORT_ID =
            "SELECT id, audience_id AS cohort_id, name, status, "
                    + "EXTRACT(EPOCH FROM created_at)::BIGINT AS created_at "
                    + "FROM audience_owners WHERE audience_id = $1 ORDER BY created_at DESC";

  private static final String INSERT_AUDIENCE_OWNER =
      "INSERT INTO audience_owners (audience_id, tenant_id, project_id, name, status) "
          + "SELECT $1, a.tenant_id, a.project_id, $2, 'ACTIVE' FROM audiences a WHERE a.id = $1";

  private static final String REMOVE_AUDIENCE_OWNER =
      "UPDATE audience_owners SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP "
          + "WHERE audience_id = $1 AND name = $2 AND status = 'ACTIVE'";

  @Override
  public Single<AudienceMeta> findById(Long cohortId) {
    return postgresReaderClient.fetchOne(FIND_COHORT_BY_ID, Tuple.of(cohortId), CohortRepositoryImpl::mapAudienceRow);
  }

  @Override
    public Single<List<CohortOwner>> findOwners(Long cohortId) {
        return postgresReaderClient.fetchAll(FIND_OWNERS_BY_COHORT_ID, Tuple.of(cohortId), CohortRepositoryImpl::mapOwnerRow);
  }

  @Override
  public Single<Boolean> addOwner(Long cohortId, String ownerEmail, String userEmail) {
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .execute(conn, INSERT_AUDIENCE_OWNER, Tuple.of(cohortId, ownerEmail))
                    .toMaybe())
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to add owner")))
        .toSingle();
  }

  @Override
  public Single<Boolean> removeOwner(Long cohortId, String ownerEmail, String userEmail) {
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .execute(conn, REMOVE_AUDIENCE_OWNER, Tuple.of(cohortId, ownerEmail))
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

  private static CohortOwner mapOwnerRow(Row row) {
    CohortOwner o = new CohortOwner();
    o.setId(row.getLong("id"));
    o.setCohortId(row.getLong("cohort_id"));
    o.setOwner(row.getString("name"));
    String status = row.getString("status");
    o.setIsRemoved(status != null && !"ACTIVE".equalsIgnoreCase(status));
    o.setRemovedBy(null);
    o.setAddedBy(null);
    o.setCreatedAt(row.getLong("created_at"));
    return o;
  }
}
