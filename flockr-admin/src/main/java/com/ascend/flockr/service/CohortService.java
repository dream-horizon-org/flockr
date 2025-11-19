package com.ascend.flockr.service;

import com.ascend.flockr.io.request.UpdateCohortOwnerRequest;
import io.reactivex.rxjava3.core.Completable;

public interface CohortService {

  Completable updateCohortOwner(Long cohortId, String userEmail, UpdateCohortOwnerRequest updateCohortOwnerRequest);
}
