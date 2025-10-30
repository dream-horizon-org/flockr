package com.ascend.flockr.io.response;

import com.ascend.flockr.io.request.DynamicExpireConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CohortInfoVerbose extends CohortInfo {

  private Long cohortId;
  private String cohortName;
  private String description;
  private Boolean expired;
  private Long expiryDate;
  private Long createdAt;
  private String createdBy;
  private Long updatedAt;
  private List<String> coOwner;
  private Long userCount;
  private DynamicExpireConfig dynamicExpireConfig;
  private String cohortType;
  private Long lastBatchExecutionTime;
  private Boolean verified;

  @JsonProperty("ruleCount")
  private Integer taskCount;

  @JsonProperty("rules")
  private List<TaskInfoVerbose> tasks;

  private List<CohortDestinationResponse> destinations;

  public CohortInfoVerbose(
      CohortInfo cohortInfo,
      List<TaskInfoVerbose> cohortTaskList,
      List<CohortDestinationResponse> cohortDestinationList) {
    this.cohortId = cohortInfo.getCohortId();
    this.cohortName = cohortInfo.getCohortName();
    this.description = cohortInfo.getDescription();
    this.expired = cohortInfo.getExpired();
    this.expiryDate = cohortInfo.getExpiryDate();
    this.createdAt = cohortInfo.getCreatedAt();
    this.createdBy = cohortInfo.getCreatedBy();
    this.coOwner = cohortInfo.getCoOwner();
    this.userCount = cohortInfo.getUserCount();
    this.taskCount = cohortTaskList.size();
    this.tasks = cohortTaskList;
    this.dynamicExpireConfig = cohortInfo.getDynamicExpireConfig();
    this.destinations = cohortDestinationList;
    this.cohortType = cohortInfo.getCohortType();
    this.updatedAt = cohortInfo.getUpdatedAt();
    this.lastBatchExecutionTime = cohortInfo.getLastBatchExecutionTime();
    this.verified = cohortInfo.getVerified();
  }
}
