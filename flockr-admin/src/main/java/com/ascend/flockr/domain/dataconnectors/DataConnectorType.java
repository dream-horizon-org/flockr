package com.ascend.flockr.domain.dataconnectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataConnectorType {
  private Long id;
  private String kind;
  private String type;
  private String displayName;
  private Boolean active;
}
