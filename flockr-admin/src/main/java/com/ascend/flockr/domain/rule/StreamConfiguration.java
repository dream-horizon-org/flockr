package com.ascend.flockr.domain.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties()
public class StreamConfiguration<T extends SourceInfo> implements RuleConfiguration<T> {
  private PatternDefinition<T> pattern;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties()
  public static class PatternDefinition<T extends SourceInfo> {
    private List<String> groupBy;
    private List<PatternStep<T>> pattern;
    private CohortFilter cohortFilter;
    private Constraint constraint;
  }

  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties()
  public static class CohortFilter {
    private List<String> belongsTo;
    private List<String> notBelongsTo;
  }

  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties()
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
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties()
  public static class PatternStep<T extends SourceInfo> {
    @Min(1)
    private Integer order;

    private StepData<T> data;
    private Contiguity contiguity;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties()
  public static class StepData<T extends SourceInfo> {
    private Quantifier quantifier;
    private List<EventDefinition<T>> event;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties()
  public static class Quantifier {
    private String conditionOperator; // e.g., "=", ">"
    private Integer conditionValue;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties()
  public static class EventDefinition<T extends SourceInfo> {
    private T sourceInfo;
    private String eventName;
    private List<EventCondition> condition;
  }

  @Data
  @NoArgsConstructor
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
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties()
  public static class EventCondition {
    private String filterType; // e.g., "event"
    private String propertyName;
    private String propertyType; // e.g., "number"
    private String conditionOperator;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties()
    public static class SingleValueOperator extends EventCondition {
      @NotEmpty private String conditionValue;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties()
    public static class ListOperator extends EventCondition {
      @NotEmpty private List<String> conditionValues;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties()
    public static class RangeOperator extends EventCondition {
      @NotEmpty private String conditionStartValue;
      @NotEmpty private String conditionEndValue;
    }
  }
}
