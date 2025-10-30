package com.ascend.flockr.io.response;

import lombok.Data;

@Data
public class CohortDestinationResponse {

  private Long destinationId;
  private String name;
  private String alias;
  private String url;
  private String description;
  private Long cohortId;
  private String eventName;
}
