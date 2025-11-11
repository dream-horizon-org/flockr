package com.ascend.flockr.io.request;

import com.ascend.flockr.domain.stream.Contiguity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

@RequiredArgsConstructor
public class CreateRulesRequest {
  private Long audienceId;
  private List<Rule> rules;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Rule {
    private String name;
    private String description;

    @JsonProperty("start_time")
    private Long startTime;

    @JsonProperty("end_time")
    private Long endTime;

    private RuleType type;

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "type")
    @JsonSubTypes({
      @JsonSubTypes.Type(value = StreamConfiguration.class, name = "STREAM"),
      @JsonSubTypes.Type(value = BatchConfiguration.class, name = "BATCH")
    })
    private RuleConfiguration configuration;
  }

  public enum RuleType {
    STREAM,
    BATCH;

    @JsonCreator
    public static RuleType fromString(String value) {
      if (value == null) return null;
      return RuleType.valueOf(value.trim().toUpperCase());
    }
  }

  public interface RuleConfiguration {}

  // ================= Stream =================

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class StreamConfiguration implements RuleConfiguration {
    private PatternDefinition pattern;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PatternDefinition {
    private List<String> groupBy;
    private List<PatternStep> pattern;
    private CohortFilter cohortFilter;
    private Constraint constraint;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CohortFilter {
    private List<String> belongsTo;
    private List<String> notBelongsTo;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Constraint {
    @NotNull private Temporal temporal;
    @NotNull private String timeUnit;
    @NotNull private Long value;
  }

  public enum Temporal {
    within
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PatternStep {
    @Min(1)
    private Integer order;

    private StepData data;
    private Contiguity contiguity;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class StepData {
    private Quantifier quantifier;
    private List<EventDefinition> event;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Quantifier {
    private String conditionOperator; // e.g., "=", ">"
    private Integer conditionValue;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class EventDefinition {
    private Long sourceId;
    private String eventName;
    private List<EventCondition> condition;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXISTING_PROPERTY,
      property = "conditionOperator",
      visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = EventCondition.RangeOperator.class, name = "><"),
    @JsonSubTypes.Type(value = EventCondition.ListOperator.class, name = "in"),
    @JsonSubTypes.Type(value = EventCondition.ListOperator.class, name = "not_in"),
    @JsonSubTypes.Type(value = EventCondition.ListOperator.class, name = "contains"),
    @JsonSubTypes.Type(value = EventCondition.ListOperator.class, name = "not_contains"),
    @JsonSubTypes.Type(value = EventCondition.SingleValueOperator.class, name = "="),
    @JsonSubTypes.Type(value = EventCondition.SingleValueOperator.class, name = "!="),
    @JsonSubTypes.Type(value = EventCondition.SingleValueOperator.class, name = "<"),
    @JsonSubTypes.Type(value = EventCondition.SingleValueOperator.class, name = ">"),
    @JsonSubTypes.Type(value = EventCondition.SingleValueOperator.class, name = ">="),
    @JsonSubTypes.Type(value = EventCondition.SingleValueOperator.class, name = "<=")
  })
  public static class EventCondition {
    private String filterType; // e.g., "event"
    private String propertyName;
    private String propertyType; // e.g., "number"
    private String conditionOperator;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SingleValueOperator extends EventCondition {
      @NotEmpty private String conditionValue;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListOperator extends EventCondition {
      @NotEmpty private List<String> conditionValues;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RangeOperator extends EventCondition {
      @NotEmpty private String conditionStartValue;

      @NotEmpty private String conditionEndValue;
    }
  }

  // ================= Batch =================

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class BatchConfiguration implements RuleConfiguration {
    private String query;
    private Long sourceId;
  }
}
