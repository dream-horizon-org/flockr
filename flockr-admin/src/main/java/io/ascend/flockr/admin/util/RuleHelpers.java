package io.ascend.flockr.admin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ascend.flockr.admin.constants.rule.RuleConstants;
import io.ascend.flockr.admin.domain.rule.*;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Row;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

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
        .tenantId(row.getString(RuleConstants.TENANT_ID))
        .projectId(row.getString(RuleConstants.PROJECT_ID))
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

  public static List<Long> extractSourceIdFromRuleMeta(RuleMeta<SourceInfo> ruleMeta) {
    List<Long> sourceIds = new ArrayList<>();
    if (ruleMeta.getRuleType() == RuleType.STREAM) {
      StreamConfiguration<SourceInfo> streamConfiguration =
          (StreamConfiguration<SourceInfo>) ruleMeta.getConfiguration();
      sourceIds =
          streamConfiguration.getPattern().getPattern().parallelStream()
              .flatMap(
                  step -> {
                    StreamConfiguration.StepData<SourceInfo> stepData = step.getData();
                    return stepData.getEvent().parallelStream()
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
}
