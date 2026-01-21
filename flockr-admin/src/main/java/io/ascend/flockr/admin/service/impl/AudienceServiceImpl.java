package io.ascend.flockr.admin.service.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.audience.AudienceType;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.domain.rule.*;
import io.ascend.flockr.admin.exception.ErrorEnum;
import io.ascend.flockr.admin.exception.ForbiddenAccessException;
import io.ascend.flockr.admin.exception.ResourceNotFoundException;
import io.ascend.flockr.admin.io.request.*;
import io.ascend.flockr.admin.io.response.AudienceDetailsResponse;
import io.ascend.flockr.admin.io.response.AudienceMetaResponse;
import io.ascend.flockr.admin.io.response.AudienceOwnerResponse;
import io.ascend.flockr.admin.io.response.PaginatedResponse;
import io.ascend.flockr.admin.io.response.RuleDetailsResponse;
import io.ascend.flockr.admin.repository.AudienceOwnerRepository;
import io.ascend.flockr.admin.repository.AudienceRepository;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.ascend.flockr.admin.repository.RuleRepository;
import io.ascend.flockr.admin.service.AudienceService;
import io.ascend.flockr.admin.util.AsyncJakartaValidationUtil;
import io.ascend.flockr.admin.util.RuleHelpers;
import io.reactivex.rxjava3.core.Single;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link AudienceService} providing audience and rule management operations.
 *
 * <p>This service handles:
 *
 * <ul>
 *   <li>Audience creation with sink associations
 *   <li>Enrichment of rule configurations with data source details
 *   <li>Batch and stream rule processing
 *   <li>Paginated audience listings with full-text search
 * </ul>
 *
 * <p>The service performs data enrichment by fetching related connector details (sources and sinks)
 * and transforming basic {@link SourceInfo} configurations into {@link SourceInfoEnriched}
 * configurations that include complete connector metadata.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AudienceServiceImpl implements AudienceService {
  private final AudienceRepository audienceRepository;
  private final AudienceOwnerRepository audienceOwnerRepository;
  private final RuleRepository ruleRepository;
  private final DataConnectorRepository dataConnectorRepository;
  private static final String DEFAULT_ACTOR = "system";
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_LIMIT = 10;
  private static final String SINK_STATUS_ACTIVE = "ACTIVE";

  /**
   * Creates a new audience using the data provided in the request.
   *
   * <p>Encrypted project identifier is applied before the audience metadata is persisted via the
   * {@link AudienceRepository}.
   *
   * <p>This method validates that all specified sink IDs exist and are active before creating the
   * audience. If any sink is not found or inactive, an appropriate error is returned.
   *
   * @param xProjectId the encrypted project identifier from the request header
   * @param request the request payload containing audience metadata and configuration
   * @param actor the email/username of the user performing the action (defaults to 'system' if
   *     null)
   * @return a {@link Single} emitting the generated audience identifier
   * @throws ResourceNotFoundException if one or more sink IDs do not exist
   * @throws com.dream11.rest.exception.RestException if one or more sinks are not active
   */
  @Override
  public Single<Long> createAudience(
      String xProjectId, CreateAudienceRequest request, String actor) {

    String createdBy = (actor != null && !actor.isBlank()) ? actor : DEFAULT_ACTOR;

    // Validate that all sinks exist and are active before creating the audience
    return validateSinksExistAndActive(request.getSinkIds())
        .flatMap(
            validatedSinks -> {
              AudienceMeta audienceMeta =
                  AudienceMeta.builder()
                      .xProjectId(xProjectId)
                      .name(request.getName())
                      .description(request.getDescription())
                      .customAudienceConfig(request.getCustomAudienceConfig())
                      .type(request.getType())
                      .expireDate(request.getExpireDate())
                      .sinks(request.getSinkIds())
                      .createdBy(createdBy)
                      .build();

              return audienceRepository.createAudience(audienceMeta);
            });
  }

  /**
   * Validates that all specified sink IDs exist and are active.
   *
   * <p>This method performs two validations:
   *
   * <ul>
   *   <li>All requested sink IDs must exist in the database
   *   <li>All found sinks must have "ACTIVE" status
   * </ul>
   *
   * @param sinkIds the list of sink IDs to validate
   * @return a {@link Single} emitting the list of validated {@link DataSinkDetails} if all checks
   *     pass
   * @throws ResourceNotFoundException if one or more sink IDs do not exist
   * @throws com.dream11.rest.exception.RestException if one or more sinks are not active
   */
  private Single<List<DataSinkDetails>> validateSinksExistAndActive(List<Long> sinkIds) {
    log.debug("Validating {} sinks for audience creation", sinkIds.size());

    return dataConnectorRepository
        .getDataSinksByIds(sinkIds)
        .flatMap(
            fetchedSinks -> {
              // Check if all requested sinks were found
              if (fetchedSinks.size() != sinkIds.size()) {
                Set<Long> foundIds =
                    fetchedSinks.stream()
                        .map(DataSinkDetails::getId)
                        .collect(java.util.stream.Collectors.toSet());
                List<Long> missingIds =
                    sinkIds.stream().filter(id -> !foundIds.contains(id)).toList();
                log.warn("Sink validation failed: missing sink IDs {}", missingIds);
                return Single.error(
                    ErrorEnum.DATA_SINK_NOT_FOUND.toException(
                        "The following sink IDs do not exist: " + missingIds));
              }

              // Check if all sinks are active
              List<DataSinkDetails> inactiveSinks =
                  fetchedSinks.stream()
                      .filter(sink -> !SINK_STATUS_ACTIVE.equalsIgnoreCase(sink.getStatus()))
                      .toList();

              if (!inactiveSinks.isEmpty()) {
                List<Long> inactiveSinkIds =
                    inactiveSinks.stream().map(DataSinkDetails::getId).toList();
                log.warn("Sink validation failed: inactive sink IDs {}", inactiveSinkIds);
                return Single.error(
                    ErrorEnum.SINK_NOT_ACTIVE.toException(
                        "The following sinks are not active: " + inactiveSinkIds));
              }

              log.debug("All {} sinks validated successfully", sinkIds.size());
              return Single.just(fetchedSinks);
            })
        .doOnError(
            error ->
                log.error(
                    "Failed to validate sinks for audience creation: {}", error.getMessage()));
  }

  /**
   * Retrieves detailed information about an audience, including sinks and rules with enriched
   * source information.
   *
   * <p>This method fetches the {@link AudienceMeta}, associated {@link DataSinkDetails}, and all
   * rules for the given audience. Rule configurations are transformed from {@link SourceInfo} to
   * {@link SourceInfoEnriched} using the corresponding {@link DataSourceDetails}.
   *
   * @param xProjectId the encrypted project identifier from the request header
   * @param audienceId the unique identifier of the audience
   * @return a {@link Single} emitting an {@link AudienceDetailsResponse} with enriched audience
   *     details
   */
  @Override
  public Single<AudienceDetailsResponse> getAudienceDetails(String xProjectId, Long audienceId) {

    Single<AudienceMeta> audienceMetaSingle =
        audienceRepository
            .getAudienceById(xProjectId, audienceId)
            .onErrorResumeNext(
                error -> {
                  if (error instanceof NoSuchElementException) {
                    return Single.error(new ResourceNotFoundException("Audience", audienceId));
                  }
                  return Single.error(error);
                })
            .cache();

    Single<List<DataSinkDetails>> sinkDetailsListSingle =
        audienceMetaSingle.map(AudienceMeta::getSinks).flatMap(this::getDataSinksInBatch);

    Single<List<RuleMeta<SourceInfo>>> ruleMetaListSingle =
        audienceMetaSingle
            .flatMap(meta -> ruleRepository.getRulesByAudienceId(xProjectId, audienceId))
            .cache();

    Single<List<DataSourceDetails>> dataSourceDetailsSingle =
        ruleMetaListSingle
            .map(
                ruleMetaList -> {
                  Set<Long> uniqueSources = new HashSet<>();
                  ruleMetaList.forEach(
                      ruleMeta ->
                          uniqueSources.addAll(RuleHelpers.extractSourceIdFromRuleMeta(ruleMeta)));
                  return new ArrayList<>(uniqueSources);
                })
            .flatMap(this::getDataSourcesInBatch);

    Single<List<RuleMeta<SourceInfoEnriched>>> ruleMetasEnriched =
        dataSourceDetailsSingle.zipWith(
            ruleMetaListSingle,
            (dataSourceDetailsList, ruleMetaList) -> {
              Map<Long, DataSourceDetails> sourceIdToDetails = new HashMap<>();
              for (DataSourceDetails sourceDetailsItem : dataSourceDetailsList) {
                sourceIdToDetails.put(sourceDetailsItem.getId(), sourceDetailsItem);
              }

              List<RuleMeta<SourceInfoEnriched>> ruleMetasSourceEnriched = new ArrayList<>();
              for (RuleMeta<SourceInfo> sourceInfoRuleMeta : ruleMetaList) {
                // Build enriched configuration by transforming SourceInfo to
                // SourceInfoEnriched
                RuleConfiguration<SourceInfoEnriched> enrichedConfig =
                    RuleHelpers.buildEnrichedConfiguration(
                        sourceInfoRuleMeta.getConfiguration(),
                        sourceIdToDetails,
                        sourceInfoRuleMeta.getRuleType());

                // Build RuleMeta<SourceInfoEnriched>
                ruleMetasSourceEnriched.add(
                    RuleMeta.<SourceInfoEnriched>builder()
                        .ruleId(sourceInfoRuleMeta.getRuleId())
                        .xProjectId(sourceInfoRuleMeta.getXProjectId())
                        .audienceId(sourceInfoRuleMeta.getAudienceId())
                        .name(sourceInfoRuleMeta.getName())
                        .description(sourceInfoRuleMeta.getDescription())
                        .startTime(sourceInfoRuleMeta.getStartTime())
                        .endTime(sourceInfoRuleMeta.getEndTime())
                        .ruleAction(sourceInfoRuleMeta.getRuleAction())
                        .status(sourceInfoRuleMeta.getStatus())
                        .ruleType(sourceInfoRuleMeta.getRuleType())
                        .configuration(enrichedConfig)
                        .createdBy(sourceInfoRuleMeta.getCreatedBy())
                        .createdAt(sourceInfoRuleMeta.getCreatedAt())
                        .updatedAt(sourceInfoRuleMeta.getUpdatedAt())
                        .build());
              }
              return ruleMetasSourceEnriched;
            });

    return Single.zip(
        audienceMetaSingle, sinkDetailsListSingle, ruleMetasEnriched, AudienceDetailsResponse::new);
  }

  /**
   * Fetches detailed information for a batch of data sinks based on their identifiers.
   *
   * <p>If the provided list of sink IDs is {@code null} or empty, this method returns a {@link
   * Single} emitting an empty list. Otherwise, it delegates to the {@link DataConnectorRepository}
   * to fetch the data and logs the outcome.
   *
   * <p>This method validates that all requested sinks are found. If any sink is missing from the
   * database, an error is emitted to ensure data consistency.
   *
   * @param sinkIds the list of sink identifiers for which details are to be fetched
   * @return a {@link Single} emitting the list of {@link DataSinkDetails} corresponding to the
   *     requested sink IDs
   * @throws RuntimeException if not all requested sinks are found in the database
   */
  private Single<List<DataSinkDetails>> getDataSinksInBatch(List<Long> sinkIds) {
    if (sinkIds == null || sinkIds.isEmpty()) {
      return Single.just(List.of());
    }
    log.debug("Fetching {} data sinks in batch", sinkIds.size());

    return dataConnectorRepository
        .getDataSinksByIds(sinkIds)
        .filter(list -> list.size() == sinkIds.size())
        .switchIfEmpty(
            Single.error(
                new ResourceNotFoundException(
                    "DATA_SINK_PARTIAL_FETCH",
                    "Some data sinks could not be found. Requested: " + sinkIds.size())))
        .doOnSuccess(sinks -> log.debug("Successfully fetched {} data sinks", sinks.size()))
        .doOnError(
            error -> log.error("Failed to fetch data sinks in batch: {}", error.getMessage()));
  }

  /**
   * Creates rules for a given audience using the data from the request.
   *
   * <p>This method first verifies that the audience exists and belongs to the same project
   * (xProjectId) before proceeding. It then validates the request asynchronously on a worker thread
   * to avoid blocking the event loop (SQL query parsing can be CPU-intensive). After validation
   * succeeds, each rule in the request is converted to a {@link RuleMeta} with {@link SourceInfo}
   * configuration and encrypted project ID, actor and status values before being persisted via the
   * {@link RuleRepository}.
   *
   * @param xProjectId the encrypted project identifier from the request header
   * @param request the request containing the audience identifier and rule definitions
   * @param actor the email/username of the user performing the action (defaults to 'system' if
   *     null)
   * @return a {@link Single} emitting {@code true} if the rules were created successfully,
   *     otherwise propagating an error
   * @throws ForbiddenAccessException if the audience does not belong to the specified xProjectId
   * @throws ResourceNotFoundException if the audience does not exist
   */
  @Override
  public Single<Boolean> createRules(String xProjectId, CreateRulesRequest request, String actor) {

    log.info("Creating rules for audience: {}", request.getAudienceId());

    String createdBy = (actor != null && !actor.isBlank()) ? actor : DEFAULT_ACTOR;

    // First verify audience exists and belongs to the same xProjectId
    return audienceRepository
        .getAudienceById(xProjectId, request.getAudienceId())
        .onErrorResumeNext(
            error -> {
              if (error instanceof NoSuchElementException) {
                log.warn(
                    "Audience {} not found or does not belong to project {}",
                    request.getAudienceId(),
                    xProjectId);
                return Single.error(
                    new ForbiddenAccessException(
                        "PROJECT_MISMATCH",
                        "Audience "
                            + request.getAudienceId()
                            + " does not exist or does not belong to the specified project"));
              }
              return Single.error(error);
            })
        // Validate that the audience type allows rules
        .flatMap(
            audience -> {
              if (AudienceType.STATIC.name().equals(audience.getType())) {
                log.warn(
                    "Cannot add rules to STATIC audience {}. Use CSV import instead.",
                    audience.getAudienceId());
                return Single.error(ErrorEnum.RULES_NOT_ALLOWED_FOR_STATIC_AUDIENCE.toException());
              }
              return Single.just(audience);
            })
        .doOnSuccess(
            audience ->
                log.debug(
                    "Verified audience {} belongs to project {} and allows rules",
                    audience.getAudienceId(),
                    xProjectId))
        // Validate request asynchronously on worker thread (non-blocking)
        .flatMap(audience -> AsyncJakartaValidationUtil.validate(request))
        .doOnSuccess(
            validRequest ->
                log.debug("Validation successful for {} rules", validRequest.getRules().size()))
        .flatMap(
            validRequest -> {
              // Convert rules to RuleMeta objects
              List<RuleMeta<SourceInfo>> list = new ArrayList<>();
              for (CreateRulesRequest.Rule rule : validRequest.getRules()) {
                RuleMeta<SourceInfo> ruleMeta =
                    RuleMeta.builder()
                        .xProjectId(xProjectId)
                        .audienceId(validRequest.getAudienceId())
                        .name(rule.getName())
                        .description(rule.getDescription())
                        .startTime(rule.getStartTime())
                        .endTime(rule.getEndTime())
                        .ruleAction(rule.getRuleAction())
                        .status(RuleStatus.SCHEDULED)
                        .ruleType(rule.getRuleType())
                        .configuration(rule.getConfiguration())
                        .createdBy(createdBy)
                        .build();
                list.add(ruleMeta);
              }

              // Persist rules to database
              return ruleRepository.createRules(list);
            })
        .doOnSuccess(
            success ->
                log.info(
                    "Successfully created {} rules for audience {}",
                    request.getRules().size(),
                    request.getAudienceId()))
        .doOnError(
            error -> {
              if (error instanceof AsyncJakartaValidationUtil.ValidationException) {
                log.warn(
                    "Validation failed for audience {}: {}",
                    request.getAudienceId(),
                    error.getMessage());
              } else if (error instanceof ForbiddenAccessException) {
                log.warn(
                    "Access denied for audience {}: {}",
                    request.getAudienceId(),
                    error.getMessage());
              } else {
                log.error(
                    "Failed to create rules for audience {}: {}",
                    request.getAudienceId(),
                    error.getMessage());
              }
            });
  }

  /**
   * Retrieves detailed information for a specific rule belonging to an audience.
   *
   * <p>The rule configuration is enriched by replacing {@link SourceInfo} entries with {@link
   * SourceInfoEnriched} using the corresponding {@link DataSourceDetails}.
   *
   * @param xProjectId the encrypted project identifier from the request header
   * @param audienceId the identifier of the audience to which the rule belongs
   * @param ruleId the identifier of the rule whose details are to be retrieved
   * @return a {@link Single} emitting a {@link RuleDetailsResponse} with enriched rule details
   */
  @Override
  public Single<RuleDetailsResponse> getRuleDetails(
      String xProjectId, Long audienceId, Long ruleId) {
    Single<RuleMeta<SourceInfo>> ruleMetaSingle =
        ruleRepository
            .getRuleById(xProjectId, ruleId)
            .onErrorResumeNext(
                error -> {
                  if (error instanceof NoSuchElementException) {
                    return Single.error(new ResourceNotFoundException("Rule", ruleId));
                  }
                  return Single.error(error);
                })
            .cache();

    Single<List<DataSourceDetails>> sourceDetails =
        ruleMetaSingle
            .map(RuleHelpers::extractSourceIdFromRuleMeta)
            .flatMap(this::getDataSourcesInBatch);

    return sourceDetails
        .zipWith(
            ruleMetaSingle,
            (sourceDetailsList, ruleMeta) -> {
              // Map sourceId to DataSourceDetails for fast lookup
              Map<Long, DataSourceDetails> sourceIdToDetails = new HashMap<>();
              for (DataSourceDetails sourceDetailsItem : sourceDetailsList) {
                sourceIdToDetails.put(sourceDetailsItem.getId(), sourceDetailsItem);
              }

              // Build enriched configuration by transforming SourceInfo to SourceInfoEnriched
              RuleConfiguration<SourceInfoEnriched> enrichedConfig =
                  RuleHelpers.buildEnrichedConfiguration(
                      ruleMeta.getConfiguration(), sourceIdToDetails, ruleMeta.getRuleType());

              // Build RuleMeta<SourceInfoEnriched>
              return RuleMeta.<SourceInfoEnriched>builder()
                  .ruleId(ruleMeta.getRuleId())
                  .xProjectId(ruleMeta.getXProjectId())
                  .audienceId(ruleMeta.getAudienceId())
                  .name(ruleMeta.getName())
                  .description(ruleMeta.getDescription())
                  .startTime(ruleMeta.getStartTime())
                  .endTime(ruleMeta.getEndTime())
                  .ruleAction(ruleMeta.getRuleAction())
                  .status(ruleMeta.getStatus())
                  .ruleType(ruleMeta.getRuleType())
                  .configuration(enrichedConfig)
                  .createdBy(ruleMeta.getCreatedBy())
                  .createdAt(ruleMeta.getCreatedAt())
                  .updatedAt(ruleMeta.getUpdatedAt())
                  .build();
            })
        .map(RuleDetailsResponse::new);
  }

  /**
   * Fetches data source details for multiple source identifiers in a single batch query.
   *
   * <p>If the provided list of source IDs is {@code null} or empty, this method returns a {@link
   * Single} emitting an empty list. Otherwise, it delegates to the {@link DataConnectorRepository}
   * and logs the outcome.
   *
   * @param sourceIds the list of data source identifiers to fetch
   * @return a {@link Single} emitting the list of {@link DataSourceDetails} for the requested
   *     identifiers
   */
  private Single<List<DataSourceDetails>> getDataSourcesInBatch(List<Long> sourceIds) {
    if (sourceIds == null || sourceIds.isEmpty()) {
      return Single.just(List.of());
    }

    log.debug("Fetching {} data sources in batch", sourceIds);
    return dataConnectorRepository
        .getDataSourcesByIds(sourceIds)
        .filter(list -> list.size() == sourceIds.size())
        .switchIfEmpty(
            Single.error(
                new ResourceNotFoundException(
                    "DATA_SOURCE_PARTIAL_FETCH",
                    "Some data sources could not be found. Requested: " + sourceIds.size())))
        .doOnSuccess(sources -> log.debug("Successfully fetched {} data sources", sources.size()))
        .doOnError(
            error -> log.error("Failed to fetch data sources in batch: {}", error.getMessage()));
  }

  /**
   * Retrieves a list of audiences with basic metadata and rule counts.
   *
   * <p>This method supports filtering by name search, creator, and verification status. Results can
   * be paginated using page (0-indexed) and pageSize parameters similar to data connector listings.
   *
   * @param xProjectId the project identifier from the request header
   * @param nameSearch optional name search filter (partial match)
   * @param createdBy optional creator filter (exact match)
   * @param verified optional verification status filter
   * @param page optional page number (0-indexed). If null or negative, defaults to 0.
   * @param pageSize optional maximum number of results per page. If null or not positive, defaults
   *     to 10.
   * @return a {@link Single} emitting a list of {@link AudienceMetaResponse} with basic metadata
   *     and rule counts
   */
  @Override
  public Single<PaginatedResponse<AudienceMetaResponse>> getAudiencesList(
      String xProjectId,
      String nameSearch,
      String createdBy,
      Boolean verified,
      Integer page,
      Integer pageSize) {

    int resolvedPageSize = (pageSize == null || pageSize <= 0) ? DEFAULT_LIMIT : pageSize;
    int resolvedPage = (page == null || page < 0) ? DEFAULT_PAGE : page;
    int offset = resolvedPage * resolvedPageSize;

    return audienceRepository
        .getAudiencesList(xProjectId, nameSearch, createdBy, verified, resolvedPageSize, offset)
        .map(
            audiences ->
                new PaginatedResponse<>(
                    new PaginatedResponse.PageInfo(
                        resolvedPage, resolvedPageSize, audiences.size() == resolvedPageSize),
                    audiences))
        .doOnSuccess(
            response ->
                log.info(
                    "Successfully fetched {} audiences",
                    response.data() != null ? response.data().size() : 0))
        .doOnError(error -> log.error("Failed to fetch audiences list: {}", error.getMessage()));
  }

  /**
   * Adds or removes a audience owner.
   *
   * <p>Preconditions: - The audience must exist and must not be expired. - The acting user (from
   * {@code actor}) must already be an owner of the audience.
   *
   * <p>Behavior: - When {@code action == add}, inserts the target owner if not already present. -
   * When {@code action == remove}, marks the target owner as removed, preventing removal of the
   * last owner.
   *
   * <p>Postconditions: - Returns a completed {@link io.reactivex.rxjava3.core.Completable} on
   * success. - Emits an {@link IllegalStateException} if the audience is expired or the user is
   * unauthorized. - Emits an {@link IllegalStateException} if the update results in no changes.
   *
   * @param xProjectId the encrypted project identifier
   * @param audienceId the identifier of the audience to update
   * @param actor the acting user's email (must already be a audience owner), defaults to 'system'
   * @param req the request containing the action (add/remove) and the target owner email
   * @return a {@link io.reactivex.rxjava3.core.Completable} that completes on success or errors on
   *     failure
   */
  @Override
  public Single<Boolean> updateAudienceOwner(
      String xProjectId, Long audienceId, String actor, UpdateAudienceOwnerRequest req) {
    String performedBy = actor != null ? actor : DEFAULT_ACTOR;
    return audienceRepository
        .getAudienceById(xProjectId, audienceId)
        .onErrorResumeNext(
            error -> {
              if (error instanceof NoSuchElementException) {
                return Single.error(new ResourceNotFoundException("Audience", audienceId));
              }
              return Single.error(error);
            })
        .flatMap(
            (AudienceMeta audience) -> {
              Long expireDate = audience.getExpireDate(); // epoch seconds as per repository mapping
              long nowSec = System.currentTimeMillis() / 1000;
              if (expireDate != null && expireDate <= nowSec) {
                return Single.error(ErrorEnum.AUDIENCE_EXPIRED.toException());
              }
              return audienceOwnerRepository
                  .findOwners(xProjectId, audienceId)
                  .flatMap(
                      owners -> {
                        // Checking if logged-in user has permission
                        boolean authorized =
                            owners.stream().anyMatch(o -> o.getOwnerEmail().equals(performedBy));
                        if (!authorized) {
                          return Single.error(
                              new ForbiddenAccessException(
                                  "NOT_AUTHORIZED",
                                  "User "
                                      + performedBy
                                      + " is not authorized to update audience owners"));
                        }
                        if (req.getAction() == UpdateAudienceOwnerAction.ADD) {
                          List<String> verifiers = List.of();
                          return validateAndAddOwner(
                              xProjectId, audienceId, req.getEmail(), verifiers, performedBy);
                        }
                        return validateAndRemoveOwner(
                            xProjectId, audienceId, req.getEmail(), performedBy);
                      });
            });
  }

  /**
   * Validates and adds a new owner to the audience.
   *
   * <p>Validations performed:
   *
   * <ul>
   *   <li>No duplicate owners - rejects if the email is already an active owner
   * </ul>
   *
   * @param xProjectId the encrypted project identifier
   * @param audienceId audience identifier
   * @param ownerEmail target owner email to add
   * @param verifiers optional list of allowed verifier emails (reserved for future use)
   * @param performedBy acting user's email (recorded as {@code added_by})
   * @return a {@link Single} emitting {@code true} if an insert occurred
   */
  private Single<Boolean> validateAndAddOwner(
      String xProjectId,
      Long audienceId,
      String ownerEmail,
      List<String> verifiers,
      String performedBy) {

    return audienceOwnerRepository
        .findOwners(xProjectId, audienceId)
        .flatMap(
            existingOwners -> {
              // Check for duplicate owners
              boolean alreadyOwner =
                  existingOwners.stream().anyMatch(o -> o.getOwnerEmail().equals(ownerEmail));
              if (alreadyOwner) {
                return Single.error(
                    ErrorEnum.DUPLICATE_OWNER.toException(
                        "User " + ownerEmail + " is already an owner of this audience"));
              }

              log.info(
                  "Adding owner {} to audience {} by user {}", ownerEmail, audienceId, performedBy);

              return audienceOwnerRepository.addOwner(
                  xProjectId, audienceId, ownerEmail, performedBy);
            });
  }

  /**
   * Validates and removes an existing owner from the audience.
   *
   * <p>Validations performed:
   *
   * <ul>
   *   <li>Target owner must exist - rejects if the email is not an active owner
   *   <li>Cannot remove the last owner - rejects if this would leave the audience with no owners
   * </ul>
   *
   * @param xProjectId the encrypted project identifier
   * @param audienceId audience identifier
   * @param ownerEmail target owner email to remove
   * @param performedBy acting user's email (recorded as {@code removed_by})
   * @return a {@link Single} emitting {@code true} if an update occurred
   */
  private Single<Boolean> validateAndRemoveOwner(
      String xProjectId, Long audienceId, String ownerEmail, String performedBy) {

    return audienceOwnerRepository
        .findOwners(xProjectId, audienceId)
        .flatMap(
            existingOwners -> {
              // Check if target owner exists
              boolean ownerExists =
                  existingOwners.stream().anyMatch(o -> o.getOwnerEmail().equals(ownerEmail));
              if (!ownerExists) {
                return Single.error(
                    ErrorEnum.OWNER_NOT_FOUND.toException(
                        "User " + ownerEmail + " is not an owner of this audience"));
              }

              // Check if this is the last owner
              if (existingOwners.size() <= 1) {
                return Single.error(ErrorEnum.LAST_OWNER.toException());
              }

              log.info(
                  "Removing owner {} from audience {} by user {}",
                  ownerEmail,
                  audienceId,
                  performedBy);

              return audienceOwnerRepository.removeOwner(
                  xProjectId, audienceId, ownerEmail, performedBy);
            });
  }

  /**
   * Retrieves all owners for a specific audience.
   *
   * <p>This method fetches all owners (active and inactive) associated with an audience and
   * transforms them into response objects.
   *
   * @param xProjectId the encrypted project identifier from the request header
   * @param audienceId the identifier of the audience
   * @return a {@link Single} emitting a list of {@link AudienceOwnerResponse}
   */
  @Override
  public Single<List<AudienceOwnerResponse>> getAudienceOwners(String xProjectId, Long audienceId) {
    // First verify audience exists
    return audienceRepository
        .getAudienceById(xProjectId, audienceId)
        .onErrorResumeNext(
            error -> {
              if (error instanceof NoSuchElementException) {
                return Single.error(new ResourceNotFoundException("Audience", audienceId));
              }
              return Single.error(error);
            })
        .flatMap(
            audience ->
                audienceOwnerRepository
                    .findOwners(xProjectId, audienceId)
                    .map(
                        owners ->
                            owners.stream()
                                .map(
                                    owner ->
                                        AudienceOwnerResponse.builder()
                                            .id(owner.getId())
                                            .audienceId(owner.getAudienceId())
                                            .ownerEmail(owner.getOwnerEmail())
                                            .status(owner.getStatus())
                                            .createdAt(owner.getCreatedAt())
                                            .updatedAt(owner.getUpdatedAt())
                                            .build())
                                .toList()))
        .doOnSuccess(
            owners ->
                log.info(
                    "Successfully fetched {} owners for audience {}", owners.size(), audienceId))
        .doOnError(
            error ->
                log.error(
                    "Failed to fetch owners for audience {}: {}", audienceId, error.getMessage()));
  }
}
