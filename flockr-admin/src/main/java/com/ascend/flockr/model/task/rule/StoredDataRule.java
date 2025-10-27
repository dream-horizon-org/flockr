package com.ascend.flockr.model.task.rule;

import com.ascend.flockr.annotation.AcceptedValues;
import com.ascend.flockr.model.task.constant.RuleType;
import com.ascend.flockr.service.ResourceTier;
import com.ascend.flockr.util.sqlparser.SQLStatementUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.reactivex.Completable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StoredDataRule extends Rule {

  @NotNull private ResourceTier resourceTier;
  @Valid private List<? extends DataStitchSourceStruct<?>> source;
  @Valid @NotEmpty private List<? extends DataStitchSinkStruct<?>> destination;
  @NotNull private String query;
  private String slackChannel;
  private long retryCount = 0;
  private Long retryDelay;

  public StoredDataRule(String jobName, RuleType ruleType) {
    super(jobName, ruleType);
  }

  @Override
  public Completable validate() {
    List<Completable> queryValidations = new ArrayList<>();
    if (source != null) {
      for (StoredDataRule.DataStitchSourceStruct<?> sourceElement : source) {
        Completable sourceQueryValidation = sourceElement.validate();
        queryValidations.add(sourceQueryValidation);
      }
    }

    Completable queryValidation = SQLStatementUtil.validate(this.query);
    queryValidations.add(queryValidation);
    return Completable.merge(queryValidations);
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
    @JsonSubTypes.Type(value = S3SourceStruct.class, name = "S3"),
    @JsonSubTypes.Type(value = RedshiftSourceStruct.class, name = "REDSHIFT")
  })
  public abstract static class DataStitchSourceStruct<T> {

    @NotNull
    @AcceptedValues(values = {"S3", "REDSHIFT"})
    private String type;

    @NotNull @Valid private T config;

    public abstract Completable validate();
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
    @JsonSubTypes.Type(value = KafkaSinkStruct.class, name = "KAFKA"),
    @JsonSubTypes.Type(value = S3SinkStruct.class, name = "S3"),
    @JsonSubTypes.Type(value = DataFeastSinkStruct.class, name = "DATAFEAST")
  })
  public abstract static class DataStitchSinkStruct<T> {

    @NotNull
    @AcceptedValues(values = {"KAFKA", "S3"})
    private String type;

    private String select;
    @NotNull @Valid private T config;

    public DataStitchSinkStruct(String type, T config) {
      this.type = type;
      this.config = config;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class KafkaSinkStruct extends DataStitchSinkStruct<KafkaTopicConfig> {

    public KafkaSinkStruct(String brokerName, String topicName) {
      super("KAFKA", new KafkaTopicConfig(brokerName, topicName));
    }

    public KafkaSinkStruct(String brokerName, String topicName, List<String> selectExpr) {
      super("KAFKA", new KafkaTopicConfig(brokerName, topicName, selectExpr));
    }

    public KafkaSinkStruct(
        String brokerName, String topicName, Map<String, Object> customOutputTemplate) {
      super("KAFKA", new KafkaTopicConfig(brokerName, topicName, customOutputTemplate));
    }
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class KafkaTopicConfig {

    @NotNull private String brokerName;
    @NotNull private String topicName;
    private Map<String, Object> customOutputTemplate;
    private List<String> selectExpr;

    public KafkaTopicConfig(
        String brokerName, String topicName, Map<String, Object> customOutputTemplate) {
      this.brokerName = brokerName;
      this.topicName = topicName;
      this.customOutputTemplate = customOutputTemplate;
    }

    public KafkaTopicConfig(String brokerName, String topicName, List<String> selectExpr) {
      this.brokerName = brokerName;
      this.topicName = topicName;
      this.selectExpr = selectExpr;
    }

    public KafkaTopicConfig(String brokerName, String topicName) {
      this.brokerName = brokerName;
      this.topicName = topicName;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DataFeastSinkStruct extends DataStitchSinkStruct<FeatureConfig> {

    public DataFeastSinkStruct(String featureGroup, String featureName) {
      super("DATAFEAST", new FeatureConfig(featureGroup, featureName));
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class FeatureConfig {

    @NotNull private String featureGroup;

    @NotNull
    @Pattern(regexp = "[a-zA-Z0-9_]*")
    private String featureName;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RedshiftSourceStruct extends DataStitchSourceStruct<RedshiftConfig> {

    public RedshiftSourceStruct(RedshiftConfig config) {
      super("REDSHIFT", config);
    }

    @Override
    public Completable validate() {
      return SQLStatementUtil.validate(this.getConfig().query);
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RedshiftConfig {

    @NotNull private String datasetReferenceName;
    @NotNull private String query;
    @NotNull private String engineName;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class S3SourceStruct extends DataStitchSourceStruct<S3SourceConfig> {

    public S3SourceStruct(S3SourceConfig config) {
      super("S3", config);
    }

    @Override
    public Completable validate() {
      return Completable.complete();
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class S3SourceConfig {

    @NotNull private String format;
    @NotNull private String datasetReferenceName;
    private String path;
    private Boolean header;
    private String delimiter;

    private String catalog;
    private String tableName;
    private String query;
    // FIXME add validation
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class S3SinkStruct extends DataStitchSinkStruct<S3SinkConfig> {

    public S3SinkStruct(S3SinkConfig config) {
      super("S3", config);
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class S3SinkConfig {

    @NotNull private String format;
    @NotNull private String mode;
    private String path;
    private List<String> selectExpr;

    private String catalog;
    private String tableName;
    private String mergeQuery;
    // FIXME add validation
  }
}
