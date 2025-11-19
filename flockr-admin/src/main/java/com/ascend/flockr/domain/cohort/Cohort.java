package com.ascend.flockr.domain.cohort;

import lombok.Data;

@Data
public class Cohort {

  private Long id;
  private String name;
  // private String ftsName;
  private String description;
  private Boolean expired;
  private Long expirationDate;
  private String client;
  private String createdBy;
  private Long createdAt;
  private Long updatedAt;
  private Long lastBatchExecutionTime;
  private Long userCount;
  //    private DynamicExpireConfig dynamicExpireConfig;
  private String cohortType;
  private Boolean verified;
}
