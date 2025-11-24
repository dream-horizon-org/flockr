package com.ascend.flockr.service;

import com.ascend.flockr.io.request.CreateAudienceRequest;
import com.ascend.flockr.io.request.CreateRulesRequest;
import com.ascend.flockr.io.request.UpdateAudienceOwnerRequest;
import com.ascend.flockr.io.response.AudienceDetailsResponse;
import com.ascend.flockr.io.response.AudienceMetaResponse;
import com.ascend.flockr.io.response.PaginatedResponse;
import com.ascend.flockr.io.response.RuleDetailsResponse;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

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

  Completable updateAudienceOwner(
      String tenantId,
      String projectId,
      Long audienceId,
      String email,
      UpdateAudienceOwnerRequest updateCohortOwnerRequest);

}
