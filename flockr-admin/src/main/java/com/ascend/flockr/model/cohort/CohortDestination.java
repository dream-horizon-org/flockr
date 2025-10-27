package com.ascend.flockr.model.cohort;

import lombok.Data;

@Data
public class CohortDestination {

  private Long id;
  private String name;
  private String alias;
  private String url;
  private String description;

  private Long cohortId;
  private String eventName;
}
