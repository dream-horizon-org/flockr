package com.ascend.flockr.domain.cohort;

import lombok.Data;

@Data
public class CohortOwner {

  private Long id;
  private Long cohortId;
  private String owner;
  private Boolean isRemoved;
  private String removedBy;
  private String addedBy;
  private Long createdAt;
}
