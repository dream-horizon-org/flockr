package com.ascend.flockr.service.web;

import com.ascend.flockr.io.response.CohortInfoVerbose;
import io.reactivex.Maybe;

public interface CohortService {
  Maybe<CohortInfoVerbose> findCohortInfoVerboseById(Long cohortId);
}
