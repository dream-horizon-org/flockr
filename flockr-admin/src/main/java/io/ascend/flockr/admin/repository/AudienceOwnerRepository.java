package io.ascend.flockr.admin.repository;

import io.ascend.flockr.admin.domain.audience.AudienceOwner;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

/**
 * Repository interface for managing audience owners.
 *
 * <p>This repository provides methods for:
 *
 * <ul>
 *   <li>Finding all owners for a specific audience
 *   <li>Adding new owners to an audience
 *   <li>Removing owners from an audience (soft delete)
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
public interface AudienceOwnerRepository {

  /**
   * Finds all active owners for a specific audience.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param audienceId the audience identifier
   * @return a Single emitting a list of active audience owners
   */
  Single<List<AudienceOwner>> findOwners(String tenantId, String projectId, Long audienceId);

  /**
   * Adds a new owner to an audience.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param audienceId the audience identifier
   * @param ownerEmail the email of the owner to add
   * @param userEmail the email of the user performing the action
   * @return a Single emitting true if the owner was successfully added
   */
  Single<Boolean> addOwner(
      String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail);

  /**
   * Removes an owner from an audience (soft delete by setting status to INACTIVE).
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param audienceId the audience identifier
   * @param ownerEmail the email of the owner to remove
   * @param userEmail the email of the user performing the action
   * @return a Single emitting true if the owner was successfully removed
   */
  Single<Boolean> removeOwner(
      String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail);
}
