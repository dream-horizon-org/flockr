package com.ascend.flockr.domain.rule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    defaultImpl = SourceInfoBasic.class // Default to SourceInfoBasic if type is missing
    )
@JsonSubTypes({
  @JsonSubTypes.Type(value = SourceInfoEnriched.class, name = "ENRICHED"),
  @JsonSubTypes.Type(value = SourceInfoBasic.class, name = "BASIC")
})
public interface SourceInfo {}
