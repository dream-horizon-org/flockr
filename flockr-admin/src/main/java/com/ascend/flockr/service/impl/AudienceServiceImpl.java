package com.ascend.flockr.service.impl;

import com.ascend.flockr.domain.audience.AudienceMeta;
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
import com.ascend.flockr.repository.AudienceQueryRepository;
import com.ascend.flockr.repository.AudienceRepository;
import com.ascend.flockr.repository.DataConnectorRepository;
import com.ascend.flockr.repository.RuleRepository;
import com.ascend.flockr.service.AudienceService;
import com.ascend.flockr.util.RuleHelpers;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AudienceServiceImpl implements AudienceService {
  private final AudienceRepository audienceRepository;
  private final RuleRepository ruleRepository;
  private final AudienceQueryRepository audienceQueryRepository;
  private final DataConnectorRepository dataConnectorRepository;
  private static final String DEFAULT_TENANT = "dummy_org";
  private static final String DEFAULT_PROJECT = "dummy_project";
  private static final String DEFAULT_CREATOR = "dummy_user";

  @Override
  public Single<Long> createAudience(CreateAudienceRequest request) {

    AudienceMeta audienceMeta =
        AudienceMeta.builder()
            .tenantId(DEFAULT_TENANT)
            .projectId(DEFAULT_PROJECT)
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
   * @param audienceId
   * @return
   */
  @Override
  public Single<AudienceDetailsResponse> getAudienceDetails(Long audienceId) {
    return null;
  }

  @Override
  public Single<Boolean> createRules(CreateRulesRequest request) {
    String tenantId = "default-tenant";
    String createdBy = "system";
    String projectId = "default_project";

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
   * Fetches data source details for multiple source IDs in a single batch query.
   *
   * @param sourceIds List of data source IDs to fetch
   * @return Single containing list of DataSourceDetails
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
   * Builds enriched configuration by merging DataSourceDetails with SourceInfoBasic.
   *
   * @param basicConfig The configuration with SourceInfoBasic
   * @param sourceDetailsMap Map of source ID to DataSourceDetails
   * @param ruleType The type of rule (BATCH or STREAM)
   * @return RuleConfiguration with SourceInfoEnriched
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

  /** Builds enriched batch configuration. */
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

  /** Builds enriched stream configuration. */
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

  /** Builds enriched pattern definition for stream configuration. */
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

  /** Builds enriched pattern step. */
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

  /** Builds enriched event definition. */
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

  /** Enriches a single SourceInfoBasic with DataSourceDetails. */
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
