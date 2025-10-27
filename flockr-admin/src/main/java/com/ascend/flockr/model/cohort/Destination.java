package com.ascend.flockr.model.cohort;

import lombok.Data;

@Data
public class Destination {

  private Long id;
  private String name;
  private String alias;
  private String url;
  private String description;
  // private Boolean active;
  private Boolean mandatory;
  private Boolean selected;
}
