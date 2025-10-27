package com.ascend.flockr.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ComaServiceConfig {
  @JsonProperty("topicname")
  private String topicName;

  @JsonProperty("brokername")
  private String brokerName;
}
