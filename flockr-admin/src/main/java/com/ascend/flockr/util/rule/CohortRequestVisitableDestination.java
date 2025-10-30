package com.ascend.flockr.util.rule;

public interface CohortRequestVisitableDestination {

  void accept(CohortRequestDestinationVisitor visitor);
}
