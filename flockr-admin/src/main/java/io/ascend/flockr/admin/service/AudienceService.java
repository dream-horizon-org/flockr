package io.ascend.flockr.admin.service;

import io.ascend.flockr.admin.io.request.CreateAudienceRequest;
import io.ascend.flockr.admin.io.request.CreateRulesRequest;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerRequest;
import io.ascend.flockr.admin.io.response.*;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

/**
 * Service interface for managing audiences and their associated rules.
 *
 * <p>This service provides operations for:
 *
 * <ul>
 *   <li>Creating and retrieving audiences
 *   <li>Creating and retrieving rules for audiences
 *   <li>Listing audiences with filtering and pagination
 * </ul>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public interface AudienceService {
  /**
   * Creates a new audience with the provided metadata.
   *
   * @param xProjectId the encrypted project identifier
   * @param request the request containing audience metadata and configuration
   * @param actor the email/username of the user performing the action (defaults to 'system' if
   *     null)
   * @return a Single emitting the created audience ID
   */
  Single<Long> createAudience(String xProjectId, CreateAudienceRequest request, String actor);

  /**
   * Retrieves detailed information about a specific audience, including associated sinks and rules.
   *
   * @param xProjectId the encrypted project identifier
   * @param audienceId the unique identifier of the audience
   * @return a Single emitting the audience details with enriched rule information
   */
  Single<AudienceDetailsResponse> getAudienceDetails(String xProjectId, Long audienceId);

  /**
   * Creates rules for a specific audience.
   *
   * @param xProjectId the encrypted project identifier
   * @param request the request containing the audience ID and rule definitions
   * @param actor the email/username of the user performing the action (defaults to 'system' if
   *     null)
   * @return a Single emitting true if rules were created successfully
   */
  Single<Boolean> createRules(String xProjectId, CreateRulesRequest request, String actor);

  /**
   * Retrieves detailed information about a specific rule within an audience.
   *
   * @param xProjectId the encrypted project identifier
   * @param audienceId the identifier of the audience containing the rule
   * @param ruleId the unique identifier of the rule
   * @return a Single emitting the rule details with enriched source information
   */
  Single<RuleDetailsResponse> getRuleDetails(String xProjectId, Long audienceId, Long ruleId);

  /**
   * Retrieves a paginated list of audiences with basic metadata and rule counts.
   *
   * <p>Supports filtering by name search, creator, and verification status.
   *
   * @param xProjectId the encrypted project identifier
   * @param nameSearch optional search term for filtering audiences by name (partial match)
   * @param createdBy optional filter for the creator username
   * @param verified optional filter for verification status
   * @param page the page number (0-indexed)
   * @param pageSize the number of results per page
   * @return a Single emitting a paginated response containing audience metadata
   */
  Single<PaginatedResponse<AudienceMetaResponse>> getAudiencesList(
      String xProjectId,
      String nameSearch,
      String createdBy,
      Boolean verified,
      Integer page,
      Integer pageSize);

  /**
   * Adds or removes an audience owner.
   *
   * <p>Validates that the acting user is an authorized owner and the audience is not expired.
   *
   * @param xProjectId the encrypted project identifier
   * @param audienceId the identifier of the audience to update
   * @param userEmail the acting user's email (must already be an owner), defaults to 'system' if
   *     null
   * @param updateAudienceOwnerRequest the request containing the action and target owner email
   * @return a Single emitting true if the update was successful, false otherwise
   */
  Single<Boolean> updateAudienceOwner(
      String xProjectId,
      Long audienceId,
      String userEmail,
      UpdateAudienceOwnerRequest updateAudienceOwnerRequest);

  /**
   * Retrieves all owners for a specific audience.
   *
   * @param xProjectId the encrypted project identifier
   * @param audienceId the identifier of the audience
   * @return a Single emitting a list of audience owners
   */
  Single<List<AudienceOwnerResponse>> getAudienceOwners(String xProjectId, Long audienceId);

    /**
     * Retrieves audit log entries for an audience, grouped by calendar date and optionally paginated.
     *
     * <p>Each group in the returned data represents a date (local system timezone) and contains a
     * list of audit items that occurred on that date. Items include action, actor (createdBy),
     * timestamp and optional details (e.g., owner email acted upon, rule action/type, error info).
     *
     * <p>Pagination behavior:
     * <ul>
     *   <li>If {@code withPagination == false}, the method fetches a single page (limit and offset
     *       derived from {@code pageSize}/{@code pageNum}) and sets {@code hasMore} based on whether
     *       the returned item count equals {@code pageSize}.</li>
     *   <li>If {@code withPagination == true}, the method also fetches the total count and computes
     *       {@code hasMore} using {@code (page + 1) * pageSize < totalCount}.</li>
     * </ul>
     *
     * @param audienceId the identifier of the audience whose audit logs are requested
     * @param pageSize the maximum number of records to return per page (defaults applied if null/invalid)
     * @param pageNum the 0-based page index (defaults applied if null/invalid)
     * @param withPagination whether to compute {@code hasMore} using a total count query
     * @return a Single emitting a {@link PaginatedResponse} whose data is a list of date-grouped
     *     {@link AuditLogResponse} entries
     */
    Single<PaginatedResponse<AuditLogResponse>> getAudienceAuditLog(
            Long audienceId, Integer pageSize, Integer pageNum, boolean withPagination);
}
