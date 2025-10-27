package com.ascend.flockr.dao.cohort;

import com.ascend.flockr.io.request.DynamicExpireConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CohortSearchByDestinationResult {

  private Long id;
  private String name;
  private String description;
  private Long lastBatchExecutionTime;
  private Double ftsScore;
  private Long userCount;
  private DynamicExpireConfig dynamicExpireConfig;
  private String cohortType;
  private Boolean verified;

  public CohortSearchByDestinationResult(
      Long id,
      String name,
      String description,
      Long lastUpdated,
      Long userCount,
      DynamicExpireConfig config,
      String cohortType,
      Boolean verified) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.lastBatchExecutionTime =
        lastUpdated != null ? Long.valueOf(lastUpdated * 1000) : lastUpdated;
    this.userCount = userCount;
    this.dynamicExpireConfig = config;
    this.cohortType = cohortType;
    this.verified = verified;
  }

  public CohortSearchByDestinationResult(
      Long id,
      String name,
      String description,
      Long lastUpdated,
      Double ftsScore,
      Long userCount,
      DynamicExpireConfig config,
      String cohortType,
      Boolean verified) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.lastBatchExecutionTime =
        lastUpdated != null ? Long.valueOf(lastUpdated * 1000) : lastUpdated;
    this.ftsScore = ftsScore;
    this.userCount = userCount;
    this.dynamicExpireConfig = config;
    this.cohortType = cohortType;
    this.verified = verified;
  }
}
