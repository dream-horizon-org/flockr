package com.ascend.flockr.service.flink;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class TriggerSavepointRequest {

  @JsonProperty("cancel-job")
  private Boolean cancel;

  @JsonProperty("target-directory")
  private String directory;
}
