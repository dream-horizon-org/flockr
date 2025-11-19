package com.ascend.flockr.domain.rule;

import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceInfoEnriched implements SourceInfo {
  private DataSourceDetails details;
}
