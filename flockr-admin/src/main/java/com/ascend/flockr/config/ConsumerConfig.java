package com.ascend.flockr.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ConsumerConfig {

  @JsonProperty("enable")
  private Boolean enable;

  @JsonProperty("groupid")
  private String groupId;

  @JsonProperty("maxpollrecords")
  private String maxPollRecords;

  @JsonProperty("maxpollintervalms")
  private String maxPollIntervalMs;

  @JsonProperty("sessiontimeoutms")
  private String sessionTimeoutMs;

  @JsonProperty("heartbeatintervalms")
  private String heartbeatIntervalMs;

  @JsonProperty("bootstrapservers")
  private String bootstrapServers;

  @JsonProperty("enableautocommit")
  private String enableAutoCommit;

  @JsonProperty("autocommitintervalms")
  private String autoCommitIntervalMs;

  @JsonProperty("autooffsetreset")
  private String autoOffsetReset;

  @JsonProperty("topic")
  private String topic;
}
