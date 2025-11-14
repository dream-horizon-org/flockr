package com.ascend.flockr.domain.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceInfoEnriched implements SourceInfo {
  private Long id;
  private String name;
  private String type;
  private Boolean active;
}
