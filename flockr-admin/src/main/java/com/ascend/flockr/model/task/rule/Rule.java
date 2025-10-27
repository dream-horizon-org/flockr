package com.ascend.flockr.model.task.rule;

import com.ascend.flockr.model.task.constant.RuleType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.reactivex.Completable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "ruleType",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = PatternSequenceRule.class, name = "sequence"),
  @JsonSubTypes.Type(value = StoredDataRule.class, name = "BATCH"),
  @JsonSubTypes.Type(value = JsonRule.class, name = "streak"),
  @JsonSubTypes.Type(value = JsonRule.class, name = "tumblingWindow"),
  @JsonSubTypes.Type(value = JsonRule.class, name = "slidingWindow"),
  @JsonSubTypes.Type(value = JsonRule.class, name = "globalCounter"),
  @JsonSubTypes.Type(value = FlashRule.class, name = "triggerBatchQuery")
})
public abstract class Rule {

  private String jobName;

  @NotNull private RuleType ruleType;

  public abstract Completable validate();
}
