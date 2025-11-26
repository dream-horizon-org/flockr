package io.ascend.flockr.admin.repository;

import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.io.response.AudienceMetaResponse;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

/**
 * Repository interface for accessing audience data from the database.
 *
 * <p>This repository provides methods for:
 *
 * <ul>
 *   <li>Creating new audiences
 *   <li>Retrieving audience details by ID
 *   <li>Querying audiences with filtering and pagination
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
public interface AudienceRepository {
  /**
   * Creates a new audience in the database.
   *
   * @param meta the audience metadata to persist
   * @return a Single emitting the generated audience ID
   */
  Single<Long> createAudience(AudienceMeta meta);

  /**
   * Retrieves an audience by its unique identifier.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param id the unique audience identifier
   * @return a Single emitting the audience metadata, or an error if not found
   */
  Single<AudienceMeta> getAudienceById(String tenantId, String projectId, Long id);

  /**
   * Retrieves a paginated list of audiences with filtering options.
   *
   * <p>This method supports full-text search on audience names, filtering by creator and
   * verification status, and pagination using limit and offset.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param nameSearch optional search term for full-text search against audience names
   * @param createdBy optional filter for the creator username
   * @param verified optional filter for verification status
   * @param limit the maximum number of results to return
   * @param offset the number of results to skip for pagination
   * @return a Single emitting a list of audience metadata responses with rule counts
   */
  Single<List<AudienceMetaResponse>> getAudiencesList(
      String tenantId,
      String projectId,
      String nameSearch,
      String createdBy,
      Boolean verified,
      Integer limit,
      Integer offset);
}
