package com.ascend.flockr.service.cohort;

import com.ascend.flockr.dao.CohortReader;
import com.ascend.flockr.io.response.CohortDestinationResponse;
import com.ascend.flockr.io.response.CohortInfoVerbose;
import com.ascend.flockr.io.response.TaskInfoVerbose;
import com.ascend.flockr.service.AdminOperation;
import com.ascend.flockr.service.web.CohortService;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;

public class CohortServiceImpl implements CohortService {

  private final CohortReader cohortReader;
  private final CohortInfoMapper cohortInfoMapper = new CohortInfoMapper();
  private final AdminOperation adminOps;

  public CohortServiceImpl(CohortReader cohortReader, AdminOperation adminOps) {
    this.cohortReader = cohortReader;
    this.adminOps = adminOps;
  }

  @Override
  public Maybe<CohortInfoVerbose> findCohortInfoVerboseById(Long cohortId) {
    return cohortReader
        .findCohortAndOwnerById(cohortId)
        .map(cohortInfoMapper)
        .flatMapSingleElement(
            cohortInfo -> {
              Single<List<TaskInfoVerbose>> cohortTaskList =
                  adminOps.findAllTaskByCohortId(cohortId);
              Single<List<CohortDestinationResponse>> cohortDestinationList =
                  cohortReader
                      .findAllActiveDestinationByCohortId(cohortId)
                      .zipWith(Single.just(cohortDestinationResponseMapper), cdrListTransformer);
              return Single.zip(
                  Single.just(cohortInfo),
                  cohortTaskList,
                  cohortDestinationList,
                  CohortInfoVerbose::new);
            });
  }
}
