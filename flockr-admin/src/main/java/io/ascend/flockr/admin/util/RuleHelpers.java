package io.ascend.flockr.admin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ascend.flockr.admin.constants.rule.RuleConstants;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.domain.rule.*;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Row;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

/**
 * @author Prithu Sharma
 * @since 1.0
 */
@UtilityClass
public final class RuleHelpers {

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Deserialize JSON string to RuleConfiguration with SourceInfo. Uses TypeReference for proper
   * generic handling.
   */
  public static RuleConfiguration<SourceInfo> deserializeRuleConfiguration(JsonObject configJson) {
    try {
      // TypeReference preserves generic type information at runtime
      return mapper.readValue(configJson.encode(), new TypeReference<>() {});
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize rule configuration: " + configJson, e);
    }
  }

  /** Serialize RuleConfiguration with SourceInfo to JSON string. */
  public static JsonObject serializeRuleConfiguration(RuleConfiguration<SourceInfo> configuration) {
    try {
      return new JsonObject(mapper.writeValueAsString(configuration));
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to serialize configuration", e);
    }
  }

  public static RuleMeta<SourceInfo> mapRuleRow(Row row) {
    RuleConfiguration<SourceInfo> configuration =
        RuleHelpers.deserializeRuleConfiguration(row.getJsonObject(RuleConstants.CONFIGURATION));

    return RuleMeta.builder()
        .ruleId(row.getLong(RuleConstants.ID))
        .audienceId(row.getLong(RuleConstants.AUDIENCE_ID))
        .xProjectId(row.getString(RuleConstants.X_PROJECT_ID))
        .name(row.getString(RuleConstants.NAME))
        .description(row.getString(RuleConstants.DESCRIPTION))
        .startTime(row.getLong(RuleConstants.START_TIME))
        .endTime(row.getLong(RuleConstants.END_TIME))
        .ruleAction(RuleAction.valueOf(row.getString(RuleConstants.RULE_ACTION)))
        .ruleType(RuleType.valueOf(row.getString(RuleConstants.RULE_TYPE)))
        .status(RuleStatus.valueOf(row.getString(RuleConstants.STATUS)))
        .configuration(configuration)
        .createdBy(row.getString(RuleConstants.CREATED_BY))
        .createdAt(row.getLong(RuleConstants.CREATED_AT))
        .updatedAt(row.getLong(RuleConstants.UPDATED_AT))
        .build();
  }

  /**
   * Maps a database row to a RuleMetaVerbose object with sink information.
   *
   * <p>This method creates a RuleMetaVerbose that extends RuleMeta and includes a list of SinkInfo
   * objects populated from the sink IDs array in the audiences table. The sink IDs array is
   * expected to be present in the row under the "sink_ids" column name (typically from a JOIN with
   * the audiences table).
   *
   * <p>This mapper is specifically designed for queries that join rules with audiences to fetch
   * sink information in a single query, supporting the two-phase enrichment pattern where sink IDs
   * are fetched first and full sink details are enriched later.
   *
   * @param row the database row containing rule data and sink IDs from the joined audiences table
   * @return a RuleMetaVerbose object with rule metadata and sink list populated with basic SinkInfo
   *     (containing only IDs)
   */
  public static RuleMetaVerbose<SourceInfo, SinkInfo> mapRuleRowWithSinkIds(Row row) {
    RuleMeta<SourceInfo> ruleMeta = mapRuleRow(row);

    // Extract sink IDs from the BIGINT array column and convert to SinkInfo list
    List<SinkInfo> sinkList = new ArrayList<>();
    Long[] sinkArray = row.getArrayOfLongs("sink_ids");
    if (sinkArray != null) {
      for (Long sinkId : sinkArray) {
        sinkList.add(SinkInfo.builder().id(sinkId).build());
      }
    }

    // Create RuleMetaVerbose and copy all fields from RuleMeta
    RuleMetaVerbose<SourceInfo, SinkInfo> ruleMetaVerbose = new RuleMetaVerbose<>();
    ruleMetaVerbose.setXProjectId(ruleMeta.getXProjectId());
    ruleMetaVerbose.setRuleId(ruleMeta.getRuleId());
    ruleMetaVerbose.setAudienceId(ruleMeta.getAudienceId());
    ruleMetaVerbose.setName(ruleMeta.getName());
    ruleMetaVerbose.setDescription(ruleMeta.getDescription());
    ruleMetaVerbose.setStartTime(ruleMeta.getStartTime());
    ruleMetaVerbose.setEndTime(ruleMeta.getEndTime());
    ruleMetaVerbose.setRuleAction(ruleMeta.getRuleAction());
    ruleMetaVerbose.setRuleType(ruleMeta.getRuleType());
    ruleMetaVerbose.setStatus(ruleMeta.getStatus());
    ruleMetaVerbose.setConfiguration(ruleMeta.getConfiguration());
    ruleMetaVerbose.setCreatedBy(ruleMeta.getCreatedBy());
    ruleMetaVerbose.setCreatedAt(ruleMeta.getCreatedAt());
    ruleMetaVerbose.setUpdatedAt(ruleMeta.getUpdatedAt());
    ruleMetaVerbose.setAudienceName(row.getString("audience_name"));
    ruleMetaVerbose.setSinkList(sinkList);

    return ruleMetaVerbose;
  }

  /**
   * Extracts all unique source IDs from a rule's configuration.
   *
   * <p>For STREAM rules, this extracts source IDs from all event definitions across all pattern
   * steps. For BATCH rules, this extracts the single source ID from the batch configuration.
   *
   * @param ruleMeta the rule metadata containing the configuration
   * @return a list of unique source IDs referenced by the rule
   */
  public static List<Long> extractSourceIdFromRuleMeta(RuleMeta<SourceInfo> ruleMeta) {
    List<Long> sourceIds = new ArrayList<>();
    if (ruleMeta.getRuleType() == RuleType.STREAM) {
      StreamConfiguration<SourceInfo> streamConfiguration =
          (StreamConfiguration<SourceInfo>) ruleMeta.getConfiguration();
      sourceIds =
          streamConfiguration.getPattern().getPattern().stream()
              .flatMap(
                  step -> {
                    StreamConfiguration.StepData<SourceInfo> stepData = step.getData();
                    return stepData.getEvent().stream()
                        .map(eventDefinition -> eventDefinition.getSourceInfo().getId());
                  })
              .distinct()
              .toList();
    } else {
      BatchConfiguration<SourceInfo> batchConfiguration =
          (BatchConfiguration<SourceInfo>) ruleMeta.getConfiguration();
      sourceIds.add(batchConfiguration.getSource().getId());
    }
    return sourceIds;
  }

  /**
   * Extracts all unique source IDs and sink IDs from a list of RuleMetaVerbose objects.
   *
   * <p>This method processes a collection of rules and aggregates:
   *
   * <ul>
   *   <li><b>Source IDs</b>: Extracted from rule configurations (STREAM rules may have multiple
   *       sources, BATCH rules have one)
   *   <li><b>Sink IDs</b>: Extracted from the sinkList field in each RuleMetaVerbose
   * </ul>
   *
   * <p>All IDs are deduplicated across the entire list of rules, making this ideal for batch
   * enrichment operations where you need to fetch unique data sources and sinks referenced by
   * multiple rules.
   *
   * @param ruleMetaList the list of verbose rule metadata containing configurations and sink lists
   * @return a SourceAndSinkIds object containing lists of unique source IDs and sink IDs
   */
  public static SourceAndSinkIds extractSourceAndSinkIdsFromRules(
      List<RuleMetaVerbose<SourceInfo, SinkInfo>> ruleMetaList) {
    Set<Long> uniqueSourceIds = new HashSet<>();
    Set<Long> uniqueSinkIds = new HashSet<>();

    for (RuleMetaVerbose<SourceInfo, SinkInfo> ruleMeta : ruleMetaList) {
      // Extract source IDs from rule configuration
      uniqueSourceIds.addAll(extractSourceIdFromRuleMeta(ruleMeta));

      // Extract sink IDs from sinkList
      if (ruleMeta.getSinkList() != null) {
        for (SinkInfo sinkInfo : ruleMeta.getSinkList()) {
          if (sinkInfo.getId() != null) {
            uniqueSinkIds.add(sinkInfo.getId());
          }
        }
      }
    }

    return new SourceAndSinkIds(new ArrayList<>(uniqueSourceIds), new ArrayList<>(uniqueSinkIds));
  }

  /**
   * Container class for holding extracted source and sink IDs.
   *
   * <p>Used by {@link #extractSourceAndSinkIdsFromRules} to return both lists of IDs in a type-safe
   * manner.
   */
  public record SourceAndSinkIds(List<Long> sourceIds, List<Long> sinkIds) {}

  /**
   * Builds an enriched rule configuration by merging {@link DataSourceDetails} into a basic
   * configuration.
   *
   * <p>Depending on the provided {@link RuleType}, this method delegates to either {@link
   * #buildEnrichedBatchConfiguration(BatchConfiguration, Map)} or {@link
   * #buildEnrichedStreamConfiguration(StreamConfiguration, Map)}.
   *
   * @param basicConfig the basic configuration containing {@link SourceInfo} references
   * @param sourceDetailsMap a map of source identifier to {@link DataSourceDetails} used for
   *     enrichment
   * @param ruleType the type of rule (e.g. {@link RuleType#BATCH} or {@link RuleType#STREAM})
   * @return a {@link RuleConfiguration} where source information is represented as {@link
   *     SourceInfoEnriched}
   */
  public static RuleConfiguration<SourceInfoEnriched> buildEnrichedConfiguration(
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
}
