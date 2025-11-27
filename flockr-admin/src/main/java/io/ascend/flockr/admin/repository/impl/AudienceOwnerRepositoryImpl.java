package io.ascend.flockr.admin.repository.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.postgres.PostgresReaderClient;
import io.ascend.flockr.admin.client.postgres.PostgresWriterClient;
import io.ascend.flockr.admin.domain.audience.AudienceOwner;
import io.ascend.flockr.admin.repository.AudienceOwnerRepository;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of {@link AudienceOwnerRepository} using PostgreSQL as the data store.
 *
 * <p>This repository manages audience owner records, supporting operations to add, remove, and
 * query owners for audiences.
 *
 * @author Flockr Team
 * @since 1.0
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AudienceOwnerRepositoryImpl implements AudienceOwnerRepository {

  private final PostgresReaderClient postgresReaderClient;
  private final PostgresWriterClient postgresWriterClient;

  private static final String FIND_OWNERS_BY_AUDIENCE_ID =
      "SELECT id, audience_id, owner_email, status, "
          + "EXTRACT(EPOCH FROM created_at)::BIGINT AS created_at, "
          + "EXTRACT(EPOCH FROM updated_at)::BIGINT AS updated_at "
          + "FROM audience_owners "
          + "WHERE audience_id = $1 AND tenant_id = $2 AND project_id = $3 AND status = 'ACTIVE' "
          + "ORDER BY created_at DESC";

  private static final String INSERT_AUDIENCE_OWNER =
      "INSERT INTO audience_owners (audience_id, tenant_id, project_id, owner_email, status) "
          + "SELECT $1, a.tenant_id, a.project_id, $4, 'ACTIVE' "
          + "FROM audiences a "
          + "WHERE a.id = $1 AND a.tenant_id = $2 AND a.project_id = $3";

  private static final String REMOVE_AUDIENCE_OWNER =
      "UPDATE audience_owners "
          + "SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP "
          + "WHERE audience_id = $1 AND tenant_id = $2 AND project_id = $3 "
          + "AND owner_email = $4 AND status = 'ACTIVE'";

  /**
   * Retrieves all active owners for a specific audience.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param audienceId the audience identifier
   * @return a Single emitting a list of active audience owners
   */
  @Override
  public Single<List<AudienceOwner>> findOwners(
      String tenantId, String projectId, Long audienceId) {
    return postgresReaderClient.fetchAll(
        FIND_OWNERS_BY_AUDIENCE_ID,
        Tuple.of(audienceId, tenantId, projectId),
        AudienceOwner::mapOwnerRow);
  }

  /**
   * Adds a new owner to an audience.
   *
   * <p>The owner is inserted with ACTIVE status. Tenant and project IDs are validated against the
   * audience record.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param audienceId the audience identifier
   * @param ownerEmail the email of the owner to add
   * @param userEmail the email of the user performing the action (for audit purposes)
   * @return a Single emitting true if the owner was successfully added
   */
  @Override
  public Single<Boolean> addOwner(
      String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail) {
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .execute(
                        conn,
                        INSERT_AUDIENCE_OWNER,
                        Tuple.of(audienceId, tenantId, projectId, ownerEmail))
                    .toMaybe())
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to add owner")))
        .toSingle();
  }

  /**
   * Removes an owner from an audience by marking them as INACTIVE.
   *
   * <p>This is a soft delete operation - the owner record remains in the database for audit
   * purposes.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param audienceId the audience identifier
   * @param ownerEmail the email of the owner to remove
   * @param userEmail the email of the user performing the action (for audit purposes)
   * @return a Single emitting true if the owner was successfully removed
   */
  @Override
  public Single<Boolean> removeOwner(
      String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail) {
    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .execute(
                        conn,
                        REMOVE_AUDIENCE_OWNER,
                        Tuple.of(audienceId, tenantId, projectId, ownerEmail))
                    .toMaybe())
        .switchIfEmpty(Maybe.error(new IllegalStateException("Failed to remove owner")))
        .toSingle();
  }
}
