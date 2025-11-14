package com.ascend.flockr.util;

import com.ascend.flockr.domain.rule.BatchConfiguration;
import com.ascend.flockr.domain.rule.RuleAction;
import com.ascend.flockr.domain.rule.RuleConfiguration;
import com.ascend.flockr.domain.rule.RuleMeta;
import com.ascend.flockr.domain.rule.RuleStatus;
import com.ascend.flockr.domain.rule.RuleType;
import com.ascend.flockr.domain.rule.SourceInfoBasic;
import com.ascend.flockr.domain.rule.StreamConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.rxjava3.sqlclient.Row;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class RuleHelpers {

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Deserialize JSON string to RuleConfiguration with SourceInfoBasic. Uses TypeReference for
   * proper generic handling.
   */
  public static RuleConfiguration<SourceInfoBasic> deserializeRuleConfiguration(String configJson) {
    try {
      // TypeReference preserves generic type information at runtime
      return mapper.readValue(configJson, new TypeReference<>() {});
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize rule configuration: " + configJson, e);
    }
  }

  /** Serialize RuleConfiguration with SourceInfoBasic to JSON string. */
  public static String serializeRuleConfiguration(
      RuleConfiguration<SourceInfoBasic> configuration) {
    try {
      return mapper.writeValueAsString(configuration);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to serialize configuration", e);
    }
  }

  public static RuleMeta<SourceInfoBasic> mapRuleRow(Row row) {
    String configJson = row.getString("configuration");
    RuleConfiguration<SourceInfoBasic> configuration =
        RuleHelpers.deserializeRuleConfiguration(configJson);

    return RuleMeta.<SourceInfoBasic>builder()
        .ruleId(row.getLong("id"))
        .audienceId(row.getLong("audience_id"))
        .tenantId(row.getString("tenant_id"))
        .name(row.getString("name"))
        .description(row.getString("description"))
        .startTime(row.getLong("start_time"))
        .endTime(row.getLong("end_time"))
        .ruleAction(RuleAction.valueOf(row.getString("rule_action")))
        .ruleType(RuleType.valueOf(row.getString("rule_type")))
        .status(RuleStatus.valueOf(row.getString("status")))
        .configuration(configuration)
        .createdBy(row.getString("created_by"))
        .createdAt(row.getLong("created_at"))
        .updatedAt(row.getLong("updated_at"))
        .build();
  }

  public static List<Long> extractSourceIdFromRuleMeta(RuleMeta<SourceInfoBasic> ruleMeta) {
    List<Long> sourceIds = new ArrayList<>();
    if (ruleMeta.getRuleType() == RuleType.STREAM) {
      StreamConfiguration<SourceInfoBasic> streamConfiguration =
          (StreamConfiguration<SourceInfoBasic>) ruleMeta.getConfiguration();
      sourceIds =
          streamConfiguration.getPattern().getPattern().parallelStream()
              .flatMap(
                  step -> {
                    StreamConfiguration.StepData<SourceInfoBasic> stepData = step.getData();
                    return stepData.getEvent().parallelStream()
                        .map(eventDefinition -> eventDefinition.getSourceInfo().getId());
                  })
              .distinct()
              .toList();
    } else {
      BatchConfiguration<SourceInfoBasic> batchConfiguration =
          (BatchConfiguration<SourceInfoBasic>) ruleMeta.getConfiguration();
      sourceIds.add(batchConfiguration.getSource().getId());
    }
    return sourceIds;
  }
}
