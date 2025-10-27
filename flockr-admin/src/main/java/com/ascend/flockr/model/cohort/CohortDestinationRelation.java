package com.ascend.flockr.model.cohort;

import java.util.Set;
import lombok.Data;

@Data
public class CohortDestinationRelation {
  private Long id;
  private String name;
  private Set<String> destinations;
}
