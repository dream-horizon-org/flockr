package com.ascend.flockr.model.task.rule;

import com.ascend.flockr.annotation.AcceptedValues;
import com.ascend.flockr.model.task.constant.*;
import com.ascend.flockr.util.rule.RuleConfigUtil;
import com.fasterxml.jackson.annotation.*;
import io.reactivex.Completable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatternSequenceRule extends Rule {

  @NotNull
  private Integer parallelism =
      RuleConfigUtil.defaultRuleConfig().getRealtime().getSequenceConfig().getParallelism();

  @NotNull private TaskType processingType;

  @Valid @NotEmpty private List<? extends FlinkSourceSinkStruct<?>> source;

  @Valid @NotEmpty private List<? extends FlinkSourceSinkStruct<?>> destination;

  @NotNull private Long watermarkDelay = RuleConfigUtil.defaultWatermarkDelay();
  @NotEmpty private List<String> keyBy;
  @Deprecated private Map<String, Map<String, List<String>>> groupByAttributeMapping;
  @NotNull @Valid private RulePattern pattern;
  @NotEmpty private String slackChannel;

  private Map<String, Map<String, List<String>>> attributeMapping;
  private Boolean notifySlack = true;

  public PatternSequenceRule(String jobName, TaskType processingType, RuleType ruleType) {
    super(jobName, ruleType);
    this.processingType = processingType;
  }

  @Override
  public Completable validate() {
    return Completable.complete();
  }

  public enum PatternType {
    sequence
  }

  public enum LogicalOperator {
    OR,
    AND
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXISTING_PROPERTY,
      property = "type",
      visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = KafkaFlinkSourceSinkStruct.class, name = "kafka"),
    @JsonSubTypes.Type(value = AudienceFlinkSourceSinkStruct.class, name = "audience")
  })
  public abstract static class FlinkSourceSinkStruct<T> {

    @NotNull
    @AcceptedValues(values = {"kafka"})
    private String type;

    @NotNull @Valid private T config;

    /* fields only applicable for sink struct */
    @Deprecated private Boolean isPropsIncluded;
    @Deprecated private Boolean isAttributePathJson;
    public List<OutputAttribute> output;

    public FlinkSourceSinkStruct(String type, T config) {
      this.type = type;
      this.config = config;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OutputAttribute {

    @NotNull
    @AcceptedValues(values = {"event", "trait", "static"})
    private String source;

    private List<AudienceEngineFunction> functions;
    @NotNull private String attributeName;
    @NotNull private Object attributePath;

    public OutputAttribute(String source, String attributeName, Object attributePath) {
      this.source = source;
      this.attributeName = attributeName;
      this.attributePath = attributePath;
    }

    public OutputAttribute(
        String source, String attributeName, List<AudienceEngineFunction> function) {
      this.source = source;
      this.attributeName = attributeName;
      this.functions = function;
    }
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AudienceEngineFunction {
    private Long sequence;
    @NotNull private String type;
    @NotNull private String unit;
    @NotNull private Long value;
    @NotNull private String inputFormat;
    @NotNull private String outputDateFormat;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class KafkaFlinkSourceSinkStruct extends FlinkSourceSinkStruct<KafkaTopicConfig> {

    public KafkaFlinkSourceSinkStruct(String brokerName, String topicName) {
      super("kafka", new KafkaTopicConfig(brokerName, topicName));
    }

    public KafkaFlinkSourceSinkStruct(
        String brokerName,
        String topicName,
        Boolean isPropsIncluded,
        Boolean isAttributePathJson,
        List<OutputAttribute> output) {
      super(
          "kafka",
          new KafkaTopicConfig(brokerName, topicName),
          isPropsIncluded,
          isAttributePathJson,
          output);
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class KafkaTopicConfig {

    @NotNull private String brokerName;
    @NotNull private String topicName;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AudienceFlinkSourceSinkStruct extends FlinkSourceSinkStruct<AudienceConfig> {

    public AudienceFlinkSourceSinkStruct(String audienceName, String action) {
      super("audience", new AudienceConfig(audienceName, action));
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AudienceConfig {

    @NotNull
    @Pattern(regexp = "[a-zA-Z0-9_]*")
    private String audienceName;

    @NotNull
    @AcceptedValues(values = {"ADD", "REMOVE"})
    private String action;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RulePattern {

    @NotNull @Valid private PatternSequence sequence;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PatternSequence {

    @NotNull @Valid private PatternStruct begin;
    @Valid Expression filters;
    @Valid private TumblingWindow within;
    private boolean filterBeforePatternComputation;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PatternStruct {

    @NotNull @Valid private PatternProperties patternProperties;
    private Contiguity nextSeqType;
    @Valid private PatternStruct nextSeqDef;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PatternProperties {

    private String quantifiers;
    @NotNull @Valid private Expression expressions;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Expression {

    @NotNull private LogicalOperator operator;
    @Valid private List<Expression> expressions;
    @Valid private List<PropertyFilter> propertyFilters;

    public Expression(LogicalOperator operator) {
      this.operator = operator;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PropertyFilter {

    @NotNull private PropertyFilterType filterType;
    @NotNull private String propertyName;

    @NotNull
    @AcceptedValues(values = {"string", "number", "boolean"})
    private String propertyType;

    @NotNull
    @AcceptedValues(
        values = {
          "=",
          "!=",
          "<",
          ">",
          "<=",
          ">=",
          "in",
          "><",
          "not_in",
          "contains",
          "not_contains"
        })
    private String comparisonType;

    private Object comparisonValue;

    private Number rangeStartValue;

    private Number rangeEndValue;

    public PropertyFilter(String propertyName, String comparisonValue) {
      this.filterType = PropertyFilterType.event;
      this.propertyName = propertyName;
      this.propertyType = PropertyType.string.literal();
      this.comparisonType = ConditionOperator.equalsTo.sign();
      this.comparisonValue = comparisonValue;
    }

    public PropertyFilter(
        String propertyName,
        PropertyType propertyType,
        ConditionOperator comparisonType,
        Object comparisonValue) {
      this.filterType = PropertyFilterType.event;
      this.propertyName = propertyName;
      this.propertyType = propertyType.literal();
      this.comparisonType = comparisonType.sign();
      this.comparisonValue = comparisonValue;
    }

    public PropertyFilter(
        PropertyFilterType filterType,
        String propertyName,
        PropertyType propertyType,
        ConditionOperator conditionOperator,
        Object comparisonValue) {
      this.filterType = filterType;
      this.propertyName = propertyName;
      this.propertyType = propertyType.literal();
      this.comparisonType = conditionOperator.sign();
      this.comparisonValue = comparisonValue;
    }

    public PropertyFilter(
        PropertyFilterType filterType,
        String propertyName,
        PropertyType propertyType,
        ConditionOperator conditionOperator,
        Number rangeStartValue,
        Number rangeEndValue) {
      this.filterType = filterType;
      this.propertyName = propertyName;
      this.propertyType = propertyType.literal();
      this.comparisonType = conditionOperator.sign();
      this.rangeStartValue = rangeStartValue;
      this.rangeEndValue = rangeEndValue;
    }

    public void setPropertyType(PropertyType propertyType) {
      this.propertyType = propertyType.literal();
    }

    @JsonSetter
    @SuppressWarnings("unused")
    private void setPropertyType(String propertyType) {
      this.propertyType = propertyType;
    }

    public void setComparisonType(ConditionOperator comparisonType) {
      this.comparisonType = comparisonType.sign();
    }

    @JsonSetter
    @SuppressWarnings("unused")
    private void setComparisonType(String comparisonType) {
      this.comparisonType = comparisonType;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TumblingWindow {

    @NotNull private TimeUnit unit;
    @NotNull private Long value;
  }
}
