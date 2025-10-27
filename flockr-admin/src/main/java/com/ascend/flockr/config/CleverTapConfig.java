package com.ascend.flockr.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CleverTapConfig {

  @JsonProperty("topicname")
  private String topicName;

  @JsonProperty("brokername")
  private String brokerName;

  @JsonProperty("connector")
  private ConnectorConfig connectorConfig;

  @Getter
  public static class ConnectorConfig {
    private String host;
    private Integer port;
    private Interval timeout;
    private String name;
  }
}
