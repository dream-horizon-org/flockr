package com.ascend.flockr.service.impl;

import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import com.ascend.flockr.domain.rule.BatchConfiguration;
import com.ascend.flockr.domain.rule.RuleConfiguration;
import com.ascend.flockr.domain.rule.RuleMeta;
import com.ascend.flockr.domain.rule.RuleStatus;
import com.ascend.flockr.domain.rule.RuleType;
import com.ascend.flockr.domain.rule.SourceInfoBasic;
import com.ascend.flockr.domain.rule.SourceInfoEnriched;
import com.ascend.flockr.domain.rule.StreamConfiguration;
import com.ascend.flockr.io.request.CreateAudienceRequest;
import com.ascend.flockr.io.request.CreateRulesRequest;
import com.ascend.flockr.io.response.AudienceDetailsResponse;
import com.ascend.flockr.io.response.RuleDetailsResponse;
import com.ascend.flockr.repository.AudienceRepository;
import com.ascend.flockr.repository.DataConnectorRepository;
import com.ascend.flockr.repository.RuleRepository;
import com.ascend.flockr.service.AudienceService;
import com.ascend.flockr.util.RuleHelpers;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AudienceServiceImpl implements AudienceService {
  private final AudienceRepository audienceRepository;
  private final RuleRepository ruleRepository;
  private final DataConnectorRepository dataConnectorRepository;
  private static final String DEFAULT_TENANT = "dummy_org";
  private static final String DEFAULT_PROJECT = "dummy_project";
  private static final String DEFAULT_CREATOR = "dummy_user";

  /**
   * Creates a new audience using the data provided in the request.
   *
   * <p>Default tenant, project and creator identifiers are applied before the audience metadata is
   * persisted via the {@link AudienceRepository}.
   *
   * @param request the request payload containing audience metadata and configuration
   * @return a {@link Single} emitting the generated audience identifier
   */
  @Override
  public Single<Long> createAudience(CreateAudienceRequest request) {
      return null;

//    AudienceMeta audienceMeta =
//        AudienceMeta.builder()
//            .tenantId(DEFAULT_TENANT)
//            .projectId(DEFAULT_PROJECT)
//            .name(request.getName())
//            .description(request.getDescription())
//            .customAudienceConfig(request.getCustomAudienceConfig())
//            .type(request.getType())
//            .expireDate(request.getExpiryDate())
//            .sinks(request.getSinkIds())
//            .createdBy(DEFAULT_CREATOR)
//            .build();
//
//    return audienceRepository.createAudience(audienceMeta);
  }

  /**
   * Retrieves detailed information about an audience, including sinks and rules with enriched
   * source information.
   *
   * <p>This method fetches the {@link AudienceMeta}, associated {@link DataSinkDetails}, and all
   * rules for the given audience. Rule configurations are transformed from {@link SourceInfoBasic}
   * to {@link SourceInfoEnriched} using the corresponding {@link DataSourceDetails}.
   *
   * @param audienceId the unique identifier of the audience
   * @return a {@link Single} emitting an {@link AudienceDetailsResponse} with enriched audience
   *     details
   */
  @Override
  public Single<AudienceDetailsResponse> getAudienceDetails(Long audienceId) {

    Single<AudienceMeta> audienceMetaSingle =
        audienceRepository.getAudienceById(audienceId).cache();

    Single<List<DataSinkDetails>> sinkDetailsListSingle =
        audienceMetaSingle.map(AudienceMeta::getSinks).flatMap(this::getDataSinksInBatch);

    Single<List<RuleMeta<SourceInfoBasic>>> ruleMetaListSingle =
        audienceMetaSingle.flatMap(meta -> ruleRepository.getRulesByAudienceId(audienceId)).cache();

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
              for (RuleMeta<SourceInfoBasic> sourceInfoBasicRuleMeta : ruleMetaList) {
                // Build enriched configuration by transforming SourceInfoBasic to
                // SourceInfoEnriched
                RuleConfiguration<SourceInfoEnriched> enrichedConfig =
                    buildEnrichedConfiguration(
                        sourceInfoBasicRuleMeta.getConfiguration(),
                        sourceIdToDetails,
                        sourceInfoBasicRuleMeta.getRuleType());

                // Build RuleMeta<SourceInfoEnriched>
                ruleMetasSourceEnriched.add(
                    RuleMeta.<SourceInfoEnriched>builder()
                        .ruleId(sourceInfoBasicRuleMeta.getRuleId())
                        .tenantId(sourceInfoBasicRuleMeta.getTenantId())
                        .projectId(sourceInfoBasicRuleMeta.getProjectId())
                        .audienceId(sourceInfoBasicRuleMeta.getAudienceId())
                        .name(sourceInfoBasicRuleMeta.getName())
                        .description(sourceInfoBasicRuleMeta.getDescription())
                        .startTime(sourceInfoBasicRuleMeta.getStartTime())
                        .endTime(sourceInfoBasicRuleMeta.getEndTime())
                        .ruleAction(sourceInfoBasicRuleMeta.getRuleAction())
                        .status(sourceInfoBasicRuleMeta.getStatus())
                        .ruleType(sourceInfoBasicRuleMeta.getRuleType())
                        .configuration(enrichedConfig)
                        .createdBy(sourceInfoBasicRuleMeta.getCreatedBy())
                        .createdAt(sourceInfoBasicRuleMeta.getCreatedAt())
                        .updatedAt(sourceInfoBasicRuleMeta.getUpdatedAt())
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
   * <p>Each rule in the request is converted to a {@link RuleMeta} with {@link SourceInfoBasic}
   * configuration and default tenant, project, creator and status values before being persisted via
   * the {@link RuleRepository}.
   *
   * @param request the request containing the audience identifier and rule definitions
   * @return a {@link Single} emitting {@code true} if the rules were created successfully,
   *     otherwise propagating an error
   */
  @Override
  public Single<Boolean> createRules(CreateRulesRequest request) {
    List<RuleMeta<SourceInfoBasic>> list = new ArrayList<>();
    for (CreateRulesRequest.Rule rule : request.getRules()) {
      RuleMeta<SourceInfoBasic> ruleMeta =
          RuleMeta.<SourceInfoBasic>builder()
              .tenantId(DEFAULT_TENANT)
              .projectId(DEFAULT_PROJECT)
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
   * <p>The rule configuration is enriched by replacing {@link SourceInfoBasic} entries with {@link
   * SourceInfoEnriched} using the corresponding {@link DataSourceDetails}.
   *
   * @param audienceId the identifier of the audience to which the rule belongs
   * @param ruleId the identifier of the rule whose details are to be retrieved
   * @return a {@link Single} emitting a {@link RuleDetailsResponse} with enriched rule details
   */
  @Override
  public Single<RuleDetailsResponse> getRuleDetails(Long audienceId, Long ruleId) {
    Single<RuleMeta<SourceInfoBasic>> ruleMetaSingle = ruleRepository.getRuleById(ruleId).cache();

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

              // Build enriched configuration by transforming SourceInfoBasic to SourceInfoEnriched
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
   * <p>Depending on the provided {@link RuleType}, this method delegates to either {@link
   * #buildEnrichedBatchConfiguration(BatchConfiguration, Map)} or {@link
   * #buildEnrichedStreamConfiguration(StreamConfiguration, Map)}.
   *
   * @param basicConfig the basic configuration containing {@link SourceInfoBasic} references
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @param ruleType the type of rule (e.g. {@link RuleType#BATCH} or {@link RuleType#STREAM})
   * @return a {@link RuleConfiguration} where source information is represented as {@link
   *     SourceInfoEnriched}
   */
  private RuleConfiguration<SourceInfoEnriched> buildEnrichedConfiguration(
      RuleConfiguration<SourceInfoBasic> basicConfig,
      Map<Long, DataSourceDetails> sourceDetailsMap,
      RuleType ruleType) {

    if (ruleType == RuleType.BATCH) {
      return buildEnrichedBatchConfiguration(
          (BatchConfiguration<SourceInfoBasic>) basicConfig, sourceDetailsMap);
    } else {
      return buildEnrichedStreamConfiguration(
          (StreamConfiguration<SourceInfoBasic>) basicConfig, sourceDetailsMap);
    }
  }

  /**
   * Builds an enriched batch configuration from a basic configuration.
   *
   * <p>The source information in the basic configuration is replaced with {@link
   * SourceInfoEnriched} using the provided {@link DataSourceDetails}.
   *
   * @param basicConfig the original batch configuration containing {@link SourceInfoBasic}
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @return a {@link BatchConfiguration} with enriched source information
   */
  private BatchConfiguration<SourceInfoEnriched> buildEnrichedBatchConfiguration(
      BatchConfiguration<SourceInfoBasic> basicConfig,
      Map<Long, DataSourceDetails> sourceDetailsMap) {

    SourceInfoBasic basicSource = basicConfig.getSource();
    SourceInfoEnriched enrichedSource = enrichSourceInfo(basicSource, sourceDetailsMap);

    return BatchConfiguration.<SourceInfoEnriched>builder()
        .type(basicConfig.getType())
        .cronExpression(basicConfig.getCronExpression())
        .query(basicConfig.getQuery())
        .source(enrichedSource)
        .build();
  }

  /**
   * Builds an enriched stream configuration from a basic configuration.
   *
   * <p>Pattern definitions are transformed so that each step and event uses {@link
   * SourceInfoEnriched} instead of {@link SourceInfoBasic}.
   *
   * @param basicConfig the original stream configuration containing {@link SourceInfoBasic}
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @return a {@link StreamConfiguration} with enriched pattern definitions
   */
  private StreamConfiguration<SourceInfoEnriched> buildEnrichedStreamConfiguration(
      StreamConfiguration<SourceInfoBasic> basicConfig,
      Map<Long, DataSourceDetails> sourceDetailsMap) {

    StreamConfiguration.PatternDefinition<SourceInfoBasic> basicPattern = basicConfig.getPattern();
    StreamConfiguration.PatternDefinition<SourceInfoEnriched> enrichedPattern =
        buildEnrichedPatternDefinition(basicPattern, sourceDetailsMap);

    return StreamConfiguration.<SourceInfoEnriched>builder()
        .type(basicConfig.getType())
        .pattern(enrichedPattern)
        .build();
  }

  /**
   * Builds an enriched pattern definition for a stream configuration.
   *
   * <p>Each pattern step is transformed to use {@link SourceInfoEnriched} while preserving the
   * grouping, filters and constraints from the basic definition.
   *
   * @param basicPattern the original pattern definition containing {@link SourceInfoBasic}
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @return a pattern definition with enriched pattern steps
   */
  private StreamConfiguration.PatternDefinition<SourceInfoEnriched> buildEnrichedPatternDefinition(
      StreamConfiguration.PatternDefinition<SourceInfoBasic> basicPattern,
      Map<Long, DataSourceDetails> sourceDetailsMap) {

    List<StreamConfiguration.PatternStep<SourceInfoEnriched>> enrichedSteps = new ArrayList<>();

    if (basicPattern.getPattern() != null) {
      for (StreamConfiguration.PatternStep<SourceInfoBasic> basicStep : basicPattern.getPattern()) {
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
   * @param basicStep the original pattern step containing {@link SourceInfoBasic}
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @return a pattern step with enriched event data
   */
  private StreamConfiguration.PatternStep<SourceInfoEnriched> buildEnrichedPatternStep(
      StreamConfiguration.PatternStep<SourceInfoBasic> basicStep,
      Map<Long, DataSourceDetails> sourceDetailsMap) {

    StreamConfiguration.StepData<SourceInfoEnriched> enrichedData = null;

    if (basicStep.getData() != null) {
      List<StreamConfiguration.EventDefinition<SourceInfoEnriched>> enrichedEvents =
          new ArrayList<>();

      if (basicStep.getData().getEvent() != null) {
        for (StreamConfiguration.EventDefinition<SourceInfoBasic> basicEvent :
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
   * @param basicEvent the original event definition containing {@link SourceInfoBasic}
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @return an event definition with enriched source information
   */
  private StreamConfiguration.EventDefinition<SourceInfoEnriched> buildEnrichedEventDefinition(
      StreamConfiguration.EventDefinition<SourceInfoBasic> basicEvent,
      Map<Long, DataSourceDetails> sourceDetailsMap) {

    SourceInfoEnriched enrichedSource =
        enrichSourceInfo(basicEvent.getSourceInfo(), sourceDetailsMap);

    StreamConfiguration.EventDefinition<SourceInfoEnriched> enrichedEvent =
        new StreamConfiguration.EventDefinition<>();
    enrichedEvent.setSourceInfo(enrichedSource);
    enrichedEvent.setEventName(basicEvent.getEventName());
    enrichedEvent.setCondition(basicEvent.getCondition());

    return enrichedEvent;
  }

  /**
   * Enriches a single {@link SourceInfoBasic} instance with {@link DataSourceDetails}.
   *
   * <p>If the basic source is {@code null}, this method returns {@code null}. If no details are
   * found for the given source identifier, the enriched source will contain only the identifier and
   * default values for other fields.
   *
   * @param basicSource the basic source information to be enriched
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @return an instance of {@link SourceInfoEnriched} with merged details, or {@code null} if the
   *     input source is {@code null}
   */
  private SourceInfoEnriched enrichSourceInfo(
      SourceInfoBasic basicSource, Map<Long, DataSourceDetails> sourceDetailsMap) {

    if (basicSource == null) {
      return null;
    }

    DataSourceDetails details = sourceDetailsMap.get(basicSource.getId());

    return SourceInfoEnriched.builder()
        .id(basicSource.getId())
        .name(details != null ? details.getName() : null)
        .type(details != null ? details.getType() : null)
        .active(details != null && "ACTIVE".equals(details.getStatus()))
        .build();
  }
}
