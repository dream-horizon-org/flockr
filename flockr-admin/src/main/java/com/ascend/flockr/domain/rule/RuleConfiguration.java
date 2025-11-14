package com.ascend.flockr.domain.rule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = StreamConfiguration.class, name = "STREAM"),
  @JsonSubTypes.Type(value = BatchConfiguration.class, name = "BATCH")
})
public interface RuleConfiguration<T extends SourceInfo> {
  String getType();
}
