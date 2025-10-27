package com.ascend.flockr.model.task.rule;

import com.ascend.flockr.annotation.AcceptedValues;
import com.ascend.flockr.model.task.constant.TaskType;
import com.ascend.flockr.util.rule.RuleConfigUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.reactivex.Completable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlashRule extends Rule {

  @NotNull
  private Integer parallelism =
      RuleConfigUtil.defaultRuleConfig().getRealtime().getFlashConfig().getParallelism();

  private TaskType processingType = TaskType.eventStream;

  @NotNull private String slackChannel;

  @NotNull private String groupId;

  private Map<String, Map<String, List<String>>> attributeMapping;

  @NotNull
  @Min(message = "Minimum SLA is 300sec", value = 300)
  private Integer sla;

  @Valid @NotEmpty private List<TriggerSourceConfig> source;

  @Valid @NotNull private RuleTriggerBatchQuery triggerBatchQuery;

  @Override
  public Completable validate() {
    return Completable.complete();
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
    @JsonSubTypes.Type(value = TriggerSourceKafkaConfig.class, name = "kafka"),
  })
  public static class TriggerSourceConfig {

    @NotNull
    @AcceptedValues(values = {"kafka"})
    private String type;
  }

  @Data
  @NoArgsConstructor
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TriggerSourceKafkaConfig extends TriggerSourceConfig {

    @NotNull @Valid private KafkaConfigSchema config;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RuleTriggerBatchQuery {

    @NotNull private String query;
    @NotNull @Valid private RetryInfo retryInfo;
    @NotNull @Valid private Map<String, ConfigSchema> source;
    @NotNull @Valid private List<ConfigSchema> destination;
    @NotNull @Valid private PatternSequenceRule.Expression expressions;

    @NotNull
    @NotEmpty
    @AcceptedValues(values = {"SMALL", "MEDIUM", "XLARGE", "XXLARGE"})
    private String resourceTier;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RetryInfo {

    @NotNull private Integer delay;
    @NotNull private Integer retries;
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
    @JsonSubTypes.Type(value = RedshiftConfig.class, name = "redshift"),
    @JsonSubTypes.Type(value = S3Config.class, name = "s3"),
    @JsonSubTypes.Type(value = DataSourceSinkKafkaConfig.class, name = "kafka")
  })
  public static class ConfigSchema {

    @NotNull
    @AcceptedValues(values = {"s3", "redshift", "kafka"})
    private String type;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DataSourceSinkKafkaConfig extends ConfigSchema {

    @NotNull @Valid private KafkaConfigSchema config;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class KafkaConfigSchema {

    @NotNull private Integer port;
    @NotNull private String brokerName;
    @NotNull private String topicName;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class S3Config extends ConfigSchema {

    @NotNull @Valid private S3ConfigSchema config;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class S3ConfigSchema {

    @NotNull private String format;

    @NotNull private String path;

    private Boolean header = true;
    private String delimiter = ",";
    private String quote;
    private String escape;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RedshiftConfig extends ConfigSchema {

    @NotNull @Valid RedshiftConfigSchema config;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RedshiftConfigSchema {

    @NotNull private String query;
    @NotNull private String engineName;
  }
}
