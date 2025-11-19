package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.client.postgres.PostgresWriterClient;
import com.ascend.flockr.domain.cohort.Cohort;
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
            "SELECT id, name, fts_name, description, is_expired AS expired, "
                    + "UNIX_TIMESTAMP(expiration_date) AS expiration_date, "
                    + "client, created_by, verified, "
                    + "UNIX_TIMESTAMP(created_at) AS created_at, "
                    + "UNIX_TIMESTAMP(updated_at) AS updated_at, "
                    + "UNIX_TIMESTAMP(last_batch_execution_time) AS last_batch_execution_time, "
                    + "user_count, dynamic_expire_config, cohort_type "
                    + "FROM cohort_master WHERE id = ?";

    private static final String FIND_OWNERS_BY_COHORT_ID =
            "SELECT id, cohort_id, owner, is_removed, removed_by, added_by, "
                    + "UNIX_TIMESTAMP(created_at) AS created_at "
                    + "FROM cohort_owner WHERE cohort_id = ? ORDER BY created_at DESC";

  private static final String INSERT_COHORT_OWNER =
      "INSERT INTO cohort_owner(cohort_id, owner, added_by) VALUES (?, ? , ?)";

  private static final String REMOVE_COHORT_OWNER =
      "UPDATE cohort_owner SET is_removed = TRUE, removed_by = ? WHERE cohort_id = ? AND owner = ?";

  @Override
  public Single<Cohort> findById(Long cohortId) {
    return postgresReaderClient.fetchOne(FIND_COHORT_BY_ID, Tuple.of(cohortId), CohortRepositoryImpl::mapCohortRow);
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
                    .execute(conn, INSERT_COHORT_OWNER, Tuple.of(cohortId, ownerEmail, userEmail))
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
                    .execute(conn, REMOVE_COHORT_OWNER, Tuple.of(userEmail, cohortId, ownerEmail))
                    .toMaybe())
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to remove owner")))
        .toSingle();
  }

  private static Cohort mapCohortRow(Row row) {
    Cohort c = new Cohort();
    c.setId(row.getLong("id"));
    c.setName(row.getString("name"));
    c.setDescription(row.getString("description"));
    c.setExpired(row.getBoolean("expired"));
    c.setExpirationDate(row.getLong("expiration_date"));
    c.setClient(row.getString("client"));
    c.setCreatedBy(row.getString("created_by"));
    c.setCreatedAt(row.getLong("created_at"));
    c.setUpdatedAt(row.getLong("updated_at"));
    c.setLastBatchExecutionTime(row.getLong("last_batch_execution_time"));
    c.setUserCount(row.getLong("user_count"));
    c.setCohortType(row.getString("cohort_type"));
    c.setVerified(row.getBoolean("verified"));
    return c;
  }

  private static CohortOwner mapOwnerRow(Row row) {
    CohortOwner o = new CohortOwner();
    o.setId(row.getLong("id"));
    o.setCohortId(row.getLong("cohort_id"));
    o.setOwner(row.getString("owner"));
    o.setIsRemoved(row.getBoolean("is_removed"));
    o.setRemovedBy(row.getString("removed_by"));
    o.setAddedBy(row.getString("added_by"));
    o.setCreatedAt(row.getLong("created_at"));
    return o;
  }
}
