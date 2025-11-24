package com.ascend.flockr.domain.cohort;

import lombok.Data;

@Data
public class AudienceOwner {

  private Long id;
  private Long audienceId;
  private String owner;
  private Boolean isRemoved;
  private String removedBy;
  private String addedBy;
  private Long createdAt;
}
