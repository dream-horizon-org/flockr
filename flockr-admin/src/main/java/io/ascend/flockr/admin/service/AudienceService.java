package io.ascend.flockr.admin.service;

import io.ascend.flockr.admin.io.request.CreateAudienceRequest;
import io.ascend.flockr.admin.io.request.CreateRulesRequest;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerRequest;
import io.ascend.flockr.admin.io.response.AudienceDetailsResponse;
import io.ascend.flockr.admin.io.response.AudienceMetaResponse;
import io.ascend.flockr.admin.io.response.AudienceOwnerResponse;
import io.ascend.flockr.admin.io.response.PaginatedResponse;
import io.ascend.flockr.admin.io.response.RuleDetailsResponse;
import io.ascend.flockr.admin.io.response.AuditLogResponse;
import io.reactivex.rxjava3.core.Completable;
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
 * @author Flockr Team
 * @since 1.0
 */
public interface AudienceService {
  /**
   * Creates a new audience with the provided metadata.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param request the request containing audience metadata and configuration
   * @return a Single emitting the created audience ID
   */
  Single<Long> createAudience(String tenantId, String projectId, CreateAudienceRequest request);

  /**
   * Retrieves detailed information about a specific audience, including associated sinks and rules.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param audienceId the unique identifier of the audience
   * @return a Single emitting the audience details with enriched rule information
   */
  Single<AudienceDetailsResponse> getAudienceDetails(
      String tenantId, String projectId, Long audienceId);

  /**
   * Creates rules for a specific audience.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param request the request containing the audience ID and rule definitions
   * @return a Single emitting true if rules were created successfully
   */
  Single<Boolean> createRules(String tenantId, String projectId, CreateRulesRequest request);

  /**
   * Retrieves detailed information about a specific rule within an audience.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param audienceId the identifier of the audience containing the rule
   * @param ruleId the unique identifier of the rule
   * @return a Single emitting the rule details with enriched source information
   */
  Single<RuleDetailsResponse> getRuleDetails(
      String tenantId, String projectId, Long audienceId, Long ruleId);

  /**
   * Retrieves a paginated list of audiences with basic metadata and rule counts.
   *
   * <p>Supports filtering by name search, creator, and verification status.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param nameSearch optional search term for filtering audiences by name (partial match)
   * @param createdBy optional filter for the creator username
   * @param verified optional filter for verification status
   * @param page the page number (0-indexed)
   * @param pageSize the number of results per page
   * @return a Single emitting a paginated response containing audience metadata
   */
  Single<PaginatedResponse<AudienceMetaResponse>> getAudiencesList(
      String tenantId,
      String projectId,
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
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param audienceId the identifier of the audience to update
   * @param userEmail the acting user's email (must already be an owner)
   * @param updateAudienceOwnerRequest the request containing the action and target owner email
   * @return a Completable that completes on success or errors on failure
   */
  Completable updateAudienceOwner(
      String tenantId,
      String projectId,
      Long audienceId,
      String userEmail,
      UpdateAudienceOwnerRequest updateAudienceOwnerRequest);

  /**
   * Retrieves all owners for a specific audience.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param audienceId the identifier of the audience
   * @return a Single emitting a list of audience owners
   */
  Single<List<AudienceOwnerResponse>> getAudienceOwners(
      String tenantId, String projectId, Long audienceId);

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
