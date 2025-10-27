package com.ascend.flockr.dao.cohort;

import com.ascend.flockr.model.cohort.Cohort;
import java.util.List;
import lombok.Data;

@Data
public class CohortOwnerInfo extends Cohort {
  private List<String> owner;
}
