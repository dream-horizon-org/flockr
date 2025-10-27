package com.ascend.flockr.dao.cohort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CohortSearchByClientResult {

  private Long cohortId;
  private Double ftsScore;

  public CohortSearchByClientResult(Long cohortId) {
    this.cohortId = cohortId;
  }
}
