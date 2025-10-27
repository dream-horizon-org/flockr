package com.ascend.flockr.io.response;

import com.ascend.flockr.io.request.DynamicExpireConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class CohortResponse {

  private Long id;
  private String name;
  private String ftsName;
  private String description;
  private Boolean expired;
  private Long expiryDate;
  private String client;
  private String createdBy;
  private Long createdAt;
  private Long updatedAt;
  private Long lastBatchExecutionTime;
  private Double ftsScore;
  private Long userCount;
  private DynamicExpireConfig dynamicExpireConfig;
  private String cohortType;
  private List<String> coOwner;
  private Boolean verified;

  @JsonProperty("ruleCount")
  private Integer taskCount;

  private List<Map<String, Object>> destinations;
}
