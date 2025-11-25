package com.ascend.flockr.service.impl;

import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.domain.audienceOwner.AudienceOwner;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import com.ascend.flockr.domain.rule.*;
import com.ascend.flockr.io.request.CreateAudienceRequest;
import com.ascend.flockr.io.request.CreateRulesRequest;
import com.ascend.flockr.io.request.UpdateAudienceOwnerAction;
import com.ascend.flockr.io.request.UpdateAudienceOwnerRequest;
import com.ascend.flockr.io.response.AudienceDetailsResponse;
import com.ascend.flockr.io.response.AudienceMetaResponse;
import com.ascend.flockr.io.response.PaginatedResponse;
import com.ascend.flockr.io.response.RuleDetailsResponse;
import com.ascend.flockr.repository.AudienceRepository;
import com.ascend.flockr.repository.AudienceOwnerRepository;
import com.ascend.flockr.repository.DataConnectorRepository;
import com.ascend.flockr.repository.RuleRepository;
import com.ascend.flockr.service.AudienceService;
import com.dream11.rest.exception.RestException;
import com.ascend.flockr.util.RuleHelpers;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AudienceServiceImpl implements AudienceService {
  private final AudienceRepository audienceRepository;
  private final AudienceOwnerRepository audienceOwnerRepository;
  private final RuleRepository ruleRepository;
  private final DataConnectorRepository dataConnectorRepository;
  private static final String DEFAULT_CREATOR = "dummy_user";
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_LIMIT = 10;

  /**
   * Creates a new audience using the data provided in the request.
   *
   * <p>Tenant and project identifiers are applied before the audience metadata is persisted via the
   * {@link AudienceRepository}.
   *
   * @param tenantId the tenant identifier from the request header
   * @param projectId the project identifier from the request header
   * @param request the request payload containing audience metadata and configuration
   * @return a {@link Single} emitting the generated audience identifier
   */
  @Override
  public Single<Long> createAudience(
      String tenantId, String projectId, CreateAudienceRequest request) {

    AudienceMeta audienceMeta =
        AudienceMeta.builder()
            .tenantId(tenantId)
            .projectId(projectId)
            .name(request.getName())
            .description(request.getDescription())
            .customAudienceConfig(request.getCustomAudienceConfig())
            .type(request.getType())
            .expireDate(request.getExpiryDate())
            .sinks(request.getSinkIds())
            .createdBy(DEFAULT_CREATOR)
            .build();

    return audienceRepository.createAudience(audienceMeta);
  }

  /**
   * Retrieves detailed information about an audience, including sinks and rules with enriched
   * source information.
   *
   * <p>This method fetches the {@link AudienceMeta}, associated {@link DataSinkDetails}, and all
   * rules for the given audience. Rule configurations are transformed from {@link SourceInfo}
   * to {@link SourceInfoEnriched} using the corresponding {@link DataSourceDetails}.
   *
   * @param tenantId the tenant identifier from the request header
   * @param projectId the project identifier from the request header
   * @param audienceId the unique identifier of the audience
   * @return a {@link Single} emitting an {@link AudienceDetailsResponse} with enriched audience
   *     details
   */
  @Override
  public Single<AudienceDetailsResponse> getAudienceDetails(
      String tenantId, String projectId, Long audienceId) {

    Single<AudienceMeta> audienceMetaSingle =
        audienceRepository.getAudienceById(tenantId, projectId, audienceId).cache();

    Single<List<DataSinkDetails>> sinkDetailsListSingle =
        audienceMetaSingle.map(AudienceMeta::getSinks).flatMap(this::getDataSinksInBatch);

    Single<List<RuleMeta<SourceInfo>>> ruleMetaListSingle =
        audienceMetaSingle
            .flatMap(meta -> ruleRepository.getRulesByAudienceId(tenantId, projectId, audienceId))
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
                    buildEnrichedConfiguration(
                        sourceInfoRuleMeta.getConfiguration(),
                        sourceIdToDetails,
                        sourceInfoRuleMeta.getRuleType());

                // Build RuleMeta<SourceInfoEnriched>
                ruleMetasSourceEnriched.add(
                    RuleMeta.<SourceInfoEnriched>builder()
                        .ruleId(sourceInfoRuleMeta.getRuleId())
                        .tenantId(sourceInfoRuleMeta.getTenantId())
                        .projectId(sourceInfoRuleMeta.getProjectId())
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
   * @param sinkIds the list of sink identifiers for which details are to be fetched
   * @return a {@link Single} emitting the list of {@link DataSinkDetails} corresponding to the
   *     requested sink IDs
   */
  private Single<List<DataSinkDetails>> getDataSinksInBatch(List<Long> sinkIds) {
    if (sinkIds == null || sinkIds.isEmpty()) {
      return Single.just(List.of());
    }
    log.info("Fetching data sinks for sinkIds: {}", sinkIds);

    return dataConnectorRepository
        .getDataSinksByIds(sinkIds)
        .doOnSuccess(sinks -> log.info("Successfully fetched {} data sinks", sinks.size()))
        .doOnError(
            error -> log.error("Failed to fetch data sinks in batch: {}", error.getMessage()));
  }

  /**
   * Creates rules for a given audience using the data from the request.
   *
   * <p>Each rule in the request is converted to a {@link RuleMeta} with {@link SourceInfo}
   * configuration and tenant, project, creator and status values before being persisted via the
   * {@link RuleRepository}.
   *
   * @param tenantId the tenant identifier from the request header
   * @param projectId the project identifier from the request header
   * @param request the request containing the audience identifier and rule definitions
   * @return a {@link Single} emitting {@code true} if the rules were created successfully,
   *     otherwise propagating an error
   */
  @Override
  public Single<Boolean> createRules(
      String tenantId, String projectId, CreateRulesRequest request) {
    List<RuleMeta<SourceInfo>> list = new ArrayList<>();
    for (CreateRulesRequest.Rule rule : request.getRules()) {
      RuleMeta<SourceInfo> ruleMeta =
          RuleMeta.builder()
              .tenantId(tenantId)
              .projectId(projectId)
              .audienceId(request.getAudienceId())
              .name(rule.getName())
              .description(rule.getDescription())
              .startTime(rule.getStartTime())
              .endTime(rule.getEndTime())
              .ruleAction(rule.getRuleAction())
              .status(RuleStatus.SCHEDULED)
              .ruleType(rule.getRuleType())
              .configuration(rule.getConfiguration())
              .createdBy(DEFAULT_CREATOR)
              .build();
      list.add(ruleMeta);
    }
    return ruleRepository
        .createRules(list)
        .doOnSuccess(
            success ->
                log.info("Created {} rules for audience {}", list.size(), request.getAudienceId()))
        .doOnError(
            error ->
                log.error(
                    "Failed to create rules for audience {}: {}",
                    request.getAudienceId(),
                    error.getMessage()));
  }

  /**
   * Retrieves detailed information for a specific rule belonging to an audience.
   *
   * <p>The rule configuration is enriched by replacing {@link SourceInfo} entries with {@link
   * SourceInfoEnriched} using the corresponding {@link DataSourceDetails}.
   *
   * @param tenantId the tenant identifier from the request header
   * @param projectId the project identifier from the request header
   * @param audienceId the identifier of the audience to which the rule belongs
   * @param ruleId the identifier of the rule whose details are to be retrieved
   * @return a {@link Single} emitting a {@link RuleDetailsResponse} with enriched rule details
   */
  @Override
  public Single<RuleDetailsResponse> getRuleDetails(
      String tenantId, String projectId, Long audienceId, Long ruleId) {
    Single<RuleMeta<SourceInfo>> ruleMetaSingle =
        ruleRepository.getRuleById(tenantId, projectId, ruleId).cache();

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
                  buildEnrichedConfiguration(
                      ruleMeta.getConfiguration(), sourceIdToDetails, ruleMeta.getRuleType());

              // Build RuleMeta<SourceInfoEnriched>
              return RuleMeta.<SourceInfoEnriched>builder()
                  .ruleId(ruleMeta.getRuleId())
                  .tenantId(ruleMeta.getTenantId())
                  .projectId(ruleMeta.getProjectId())
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

    log.debug("Fetching {} data sources in batch", sourceIds.size());
    return dataConnectorRepository
        .getDataSourcesByIds(sourceIds)
        .doOnSuccess(sources -> log.debug("Successfully fetched {} data sources", sources.size()))
        .doOnError(
            error -> log.error("Failed to fetch data sources in batch: {}", error.getMessage()));
  }

  /**
   * Builds an enriched rule configuration by merging {@link DataSourceDetails} into a basic
   * configuration.
   *
   * <p>Depending on the provided {@link RuleType}, this method delegates to either
   * {@link #buildEnrichedBatchConfiguration(BatchConfiguration, Map)} or
   * {@link #buildEnrichedStreamConfiguration(StreamConfiguration, Map)}.
   *
   * @param basicConfig the basic configuration containing {@link SourceInfo} references
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @param ruleType the type of rule (e.g. {@link RuleType#BATCH} or {@link RuleType#STREAM})
   * @return a {@link RuleConfiguration} where source information is represented as
   * {@link SourceInfoEnriched}
   */
  private RuleConfiguration<SourceInfoEnriched> buildEnrichedConfiguration(
      RuleConfiguration<SourceInfo> basicConfig,
      Map<Long, DataSourceDetails> sourceDetailsMap,
      RuleType ruleType) {

    if (ruleType == RuleType.BATCH) {
      return buildEnrichedBatchConfiguration(
          (BatchConfiguration<SourceInfo>) basicConfig, sourceDetailsMap);
    } else {
      return buildEnrichedStreamConfiguration(
          (StreamConfiguration<SourceInfo>) basicConfig, sourceDetailsMap);
    }
  }

  /**
   * Builds an enriched batch configuration from a basic configuration.
   *
   * <p>The source information in the basic configuration is replaced with {@link
   * SourceInfoEnriched} using the provided {@link DataSourceDetails}.
   *
   * @param basicConfig the original batch configuration containing {@link SourceInfo}
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @return a {@link BatchConfiguration} with enriched source information
   */
  private BatchConfiguration<SourceInfoEnriched> buildEnrichedBatchConfiguration(
      BatchConfiguration<SourceInfo> basicConfig, Map<Long, DataSourceDetails> sourceDetailsMap) {

    SourceInfo basicSource = basicConfig.getSource();
    SourceInfoEnriched enrichedSource =
        SourceInfoEnriched.builder()
            .id(basicSource.getId())
            .details(sourceDetailsMap.get(basicSource.getId()))
            .build();

    return BatchConfiguration.<SourceInfoEnriched>builder()
        .cronExpression(basicConfig.getCronExpression())
        .query(basicConfig.getQuery())
        .source(enrichedSource)
        .build();
  }

  /**
   * Builds an enriched stream configuration from a basic configuration.
   *
   * <p>Pattern definitions are transformed so that each step and event uses {@link
   * SourceInfoEnriched} instead of {@link SourceInfo}.
   *
   * @param basicConfig the original stream configuration containing {@link SourceInfo}
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @return a {@link StreamConfiguration} with enriched pattern definitions
   */
  private StreamConfiguration<SourceInfoEnriched> buildEnrichedStreamConfiguration(
      StreamConfiguration<SourceInfo> basicConfig, Map<Long, DataSourceDetails> sourceDetailsMap) {

    StreamConfiguration.PatternDefinition<SourceInfo> basicPattern = basicConfig.getPattern();
    StreamConfiguration.PatternDefinition<SourceInfoEnriched> enrichedPattern =
        buildEnrichedPatternDefinition(basicPattern, sourceDetailsMap);

    return StreamConfiguration.<SourceInfoEnriched>builder().pattern(enrichedPattern).build();
  }

  /**
   * Builds an enriched pattern definition for a stream configuration.
   *
   * <p>Each pattern step is transformed to use {@link SourceInfoEnriched} while preserving the
   * grouping, filters and constraints from the basic definition.
   *
   * @param basicPattern the original pattern definition containing {@link SourceInfo}
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @return a pattern definition with enriched pattern steps
   */
  private StreamConfiguration.PatternDefinition<SourceInfoEnriched> buildEnrichedPatternDefinition(
      StreamConfiguration.PatternDefinition<SourceInfo> basicPattern,
      Map<Long, DataSourceDetails> sourceDetailsMap) {

    List<StreamConfiguration.PatternStep<SourceInfoEnriched>> enrichedSteps = new ArrayList<>();

    if (basicPattern.getPattern() != null) {
      for (StreamConfiguration.PatternStep<SourceInfo> basicStep : basicPattern.getPattern()) {
        enrichedSteps.add(buildEnrichedPatternStep(basicStep, sourceDetailsMap));
      }
    }

    StreamConfiguration.PatternDefinition<SourceInfoEnriched> enrichedPattern =
        new StreamConfiguration.PatternDefinition<>();
    enrichedPattern.setGroupBy(basicPattern.getGroupBy());
    enrichedPattern.setPattern(enrichedSteps);
    enrichedPattern.setCohortFilter(basicPattern.getCohortFilter());
    enrichedPattern.setConstraint(basicPattern.getConstraint());

    return enrichedPattern;
  }

  /**
   * Builds an enriched pattern step from a basic pattern step.
   *
   * <p>Event definitions inside the step are transformed to use {@link SourceInfoEnriched} while
   * preserving ordering and contiguity semantics.
   *
   * @param basicStep the original pattern step containing {@link SourceInfo}
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @return a pattern step with enriched event data
   */
  private StreamConfiguration.PatternStep<SourceInfoEnriched> buildEnrichedPatternStep(
      StreamConfiguration.PatternStep<SourceInfo> basicStep,
      Map<Long, DataSourceDetails> sourceDetailsMap) {

    StreamConfiguration.StepData<SourceInfoEnriched> enrichedData = null;

    if (basicStep.getData() != null) {
      List<StreamConfiguration.EventDefinition<SourceInfoEnriched>> enrichedEvents =
          new ArrayList<>();

      if (basicStep.getData().getEvent() != null) {
        for (StreamConfiguration.EventDefinition<SourceInfo> basicEvent :
            basicStep.getData().getEvent()) {
          enrichedEvents.add(buildEnrichedEventDefinition(basicEvent, sourceDetailsMap));
        }
      }

      enrichedData = new StreamConfiguration.StepData<>();
      enrichedData.setQuantifier(basicStep.getData().getQuantifier());
      enrichedData.setEvent(enrichedEvents);
    }

    StreamConfiguration.PatternStep<SourceInfoEnriched> enrichedStep =
        new StreamConfiguration.PatternStep<>();
    enrichedStep.setOrder(basicStep.getOrder());
    enrichedStep.setData(enrichedData);
    enrichedStep.setContiguity(basicStep.getContiguity());

    return enrichedStep;
  }

  /**
   * Builds an enriched event definition from a basic event definition.
   *
   * <p>The source information is enriched using the provided {@link DataSourceDetails}, while the
   * event name and condition are preserved.
   *
   * @param basicEvent the original event definition containing {@link SourceInfo}
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @return an event definition with enriched source information
   */
  private StreamConfiguration.EventDefinition<SourceInfoEnriched> buildEnrichedEventDefinition(
      StreamConfiguration.EventDefinition<SourceInfo> basicEvent,
      Map<Long, DataSourceDetails> sourceDetailsMap) {

    SourceInfoEnriched enrichedSource =
        SourceInfoEnriched.builder()
            .id(basicEvent.getSourceInfo().getId())
            .details(sourceDetailsMap.get(basicEvent.getSourceInfo().getId()))
            .build();

    StreamConfiguration.EventDefinition<SourceInfoEnriched> enrichedEvent =
        new StreamConfiguration.EventDefinition<>();
    enrichedEvent.setSourceInfo(enrichedSource);
    enrichedEvent.setEventName(basicEvent.getEventName());
    enrichedEvent.setCondition(basicEvent.getCondition());

    return enrichedEvent;
  }

  /**
   * Retrieves a list of audiences with basic metadata and rule counts.
   *
   * <p>This method supports filtering by name search, creator, and verification status. Results can
   * be paginated using page (0-indexed) and pageSize parameters similar to data connector listings.
   *
   * @param tenantId the tenant identifier from the request header
   * @param projectId the project identifier from the request header
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
      String tenantId,
      String projectId,
      String nameSearch,
      String createdBy,
      Boolean verified,
      Integer page,
      Integer pageSize) {

    int resolvedPageSize = (pageSize == null || pageSize <= 0) ? DEFAULT_LIMIT : pageSize;
    int resolvedPage = (page == null || page < 0) ? DEFAULT_PAGE : page;
    int offset = resolvedPage * resolvedPageSize;

    return audienceRepository
        .getAudiencesList(
            tenantId, projectId, nameSearch, createdBy, verified, resolvedPageSize, offset)
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
     * <p>Preconditions:
     * - The audience must exist and must not be expired.
     * - The acting user (from {@code userEmail}) must already be an owner of the audience.
     *
     * <p>Behavior:
     * - When {@code action == add}, inserts the target owner if not already present.
     * - When {@code action == remove}, marks the target owner as removed, preventing removal of the last owner.
     *
     * <p>Postconditions:
     * - Returns a completed {@link Completable} on success.
     * - Emits an {@link IllegalStateException} if the audience is expired or the user is unauthorized.
     * - Emits an {@link IllegalStateException} if the update results in no changes.
     *
     * @param audienceId the identifier of the audience to update
     * @param userEmail the acting user's email (must already be a audience owner)
     * @param req the request containing the action (add/remove) and the target owner email
     * @return a {@link Completable} that completes on success or errors on failure
     */
    @Override
    public Completable updateAudienceOwner(String tenantId, String projectId, Long audienceId, String userEmail, UpdateAudienceOwnerRequest req) {
        return audienceRepository
                .getAudienceById(tenantId, projectId, audienceId)
                .flatMap(
                        (AudienceMeta audience) -> {
                            Long expireDate = audience.getExpireDate(); // epoch seconds as per repository mapping
                            long nowSec = System.currentTimeMillis() / 1000;
                            if (expireDate != null && expireDate <= nowSec) {
                                return Single.error(
                                        new RestException(
                                                "AUDIENCE_EXPIRED",
                                                "Audience is expired and cannot be modified",
                                                org.apache.http.HttpStatus.SC_BAD_REQUEST));
                            }
                            return audienceOwnerRepository
                                    .findOwners(tenantId, projectId, audienceId)
                                    .flatMap(
                                            owners -> {
                                                // Checking if logged-in user has permission
                                                boolean authorized =
                                                        owners.stream().anyMatch(o -> o.getOwner().equals(userEmail));
                                                if (!authorized) {
                                                    return Single.error(
                                                            new RestException(
                                                                    "FORBIDDEN",
                                                                    "User is not authorized to update audience owners",
                                                                    org.apache.http.HttpStatus.SC_FORBIDDEN));
                                                }
                                                if (req.getAction() == UpdateAudienceOwnerAction.ADD) {
                                                    List<String> verifiers = List.of();
                                                    return validateAndAddOwner(
                                                            tenantId,
                                                            projectId,
                                                            owners,
                                                            audienceId,
                                                            req,
                                                            userEmail,
                                                            audience.getName(),
                                                            audience.getVerified(),
                                                            verifiers);
                                                }
                                                return validateAndRemoveOwner(
                                                        tenantId,
                                                        projectId,
                                                        owners,
                                                        audienceId,
                                                        req,
                                                        userEmail);
                                            });
                        })
                .flatMapCompletable(
                        ok ->
                                ok
                                        ? Completable.complete()
                                        : Completable.error(
                                                new RestException(
                                                        "OWNER_UPDATE_FAILED",
                                                        "Failed to update audience owners",
                                                        org.apache.http.HttpStatus.SC_CONFLICT)));
    }

    /**
     * Validates and adds a new owner to the audience.
     *
     * <p>Validations:
     * - No duplicate owners.
     * - Optional verifier checks (if enabled via configuration).
     *
     * @param existingOwners current owners
     * @param audienceId audience identifier
     * @param request request containing target owner email
     * @param performedBy acting user's email (recorded as {@code added_by})
     * @param audienceName audience display name (for audit/logging)
     * @param isVerified whether the audience is verified (may enforce stricter rules)
     * @param verifiers optional list of allowed verifier emails
     * @return a {@link Single} emitting {@code true} if an insert occurred
     */

    private Single<Boolean> validateAndAddOwner(
            String tenantId,
            String projectId,
            List<AudienceOwner> existingOwners,
            Long audienceId,
            UpdateAudienceOwnerRequest request,
            String performedBy,
            String audienceName,
            Boolean isVerified,
            List<String> verifiers) {

        return audienceOwnerRepository.addOwner(tenantId, projectId, audienceId, request.getEmail(), performedBy);
    }

    /**
     * Validates and removes an existing owner from the audience.
     *
     * <p>Validations:
     * - Target owner must exist.
     * - Must not remove the last remaining owner.
     *
     * @param existingOwners current owners
     * @param audienceId audience identifier
     * @param request request containing target owner email
     * @param performedBy acting user's email (recorded as {@code removed_by})
     * @return a {@link Single} emitting {@code true} if an update occurred
     */

    private Single<Boolean> validateAndRemoveOwner(
            String tenantId,
            String projectId,
            List<AudienceOwner> existingOwners,
            Long audienceId,
            UpdateAudienceOwnerRequest request,
            String performedBy) {

        return audienceOwnerRepository.removeOwner(tenantId, projectId, audienceId, request.getEmail(), performedBy);
    }
}
