package com.ascend.flockr.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class UserCohortConfig {

  @JsonProperty("topicname")
  private String topicNamePrefix;

  @JsonProperty("brokername")
  private String brokerName;

  @JsonProperty("inactivetopicretentionindays")
  private Integer inactiveTopicRetentionInDays;

  @JsonProperty("maxtopicstobedeleted")
  private Integer maxTopicsToBeDeleted;
}
