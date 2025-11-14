package com.ascend.flockr.domain.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchConfiguration<T extends SourceInfo> implements RuleConfiguration<T> {
  @Builder.Default private String type = "BATCH";

  private String cronExpression;
  private String query;
  private T source;
}
