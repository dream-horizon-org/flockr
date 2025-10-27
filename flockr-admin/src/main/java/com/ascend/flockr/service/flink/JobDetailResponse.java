package com.ascend.flockr.service.flink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDetailResponse {

  @JsonProperty("jid")
  private String jobId;

  private String name;
  private Long duration;
  private FlinkJobState state;

  @JsonProperty("start-time")
  private Long startTime;

  @JsonProperty("end-time")
  private Long endTime;

  @JsonSetter
  @SuppressWarnings("unused")
  private void setState(String state) {
    this.state = FlinkJobState.valueOf(state);
  }
}
