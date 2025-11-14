package com.ascend.flockr.domain.rule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.DEDUCTION // Auto-detect based on available fields
    )
@JsonSubTypes({
  @JsonSubTypes.Type(value = SourceInfoEnriched.class), // Try this first (more fields)
  @JsonSubTypes.Type(value = SourceInfoBasic.class) // Fallback to this
})
public interface SourceInfo {}
