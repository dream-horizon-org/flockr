package com.ascend.flockr.dao;

import com.ascend.flockr.dao.cohort.CohortOwnerInfo;
import com.ascend.flockr.dao.cohort.CohortSearchByClientResult;
import com.ascend.flockr.dao.cohort.CohortSearchByDestinationResult;
import com.ascend.flockr.io.response.CohortResponse;
import com.ascend.flockr.model.cohort.*;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;

public interface CohortReader {

  Maybe<Cohort> findById(Long cohortId);

  Single<Map<Long, CohortResponse>> findAllByIdIn(List<Long> cohortIdList);

  Single<List<CohortSearchByClientResult>> searchByClientAndOwner(
      String searchName,
      String userEmail,
      String client,
      Integer isExpired,
      Integer pageSize,
      Integer pageNum,
      Boolean verified);

  Single<List<CohortSearchByClientResult>> searchByClient(
      String searchName,
      String client,
      Integer isExpired,
      Integer pageSize,
      Integer pageNum,
      Boolean verified);

  Single<List<CohortSearchByDestinationResult>> searchByDestination(
      String searchName,
      String destinationName,
      Integer isExpired,
      Integer pageSize,
      Integer pageNum);

  Single<Map<String, Integer>> findIsExpiredGroupCountByClient(String client);

  Single<List<Destination>> findAllActiveDestination();

  Maybe<CohortOwnerInfo> findCohortAndOwnerById(Long cohortId);

  Single<List<CohortDestination>> findAllActiveDestinationByCohortId(Long cohortId);

  Single<Integer> findCohortCountByNameSearchAndClientAndOwnerAndIsExpiredAndVerified(
      String searchName, String userEmail, String client, Integer isExpired, Boolean verified);

  Single<Integer> findCohortCountByNameSearchAndClientAndIsExpired(
      String searchName, String client, Integer isExpired, Boolean verified);

  Single<Integer> findCohortCountByNameSearchAndDestinationAndIsExpired(
      String searchName, String destination, Integer isExpired);

  Single<Map<Long, List<Map<String, Object>>>> findAllDestinationByCohortIdInGroupByCohortId(
      List<Long> cohortIdList);

  Single<List<CohortDestinationRelation>> findCdrByPastExpirationDateAndExpiredFalse();

  Single<List<CohortOwnerInfo>> findAllCohortExpiringInGivenDays(Integer days);

  Single<List<CohortOwner>> findCohortOwnerByCohortId(Long cohortId);

  Single<Map<Long, List<String>>> findAllCohortOwnerByIdIn(List<Long> cohortId);

  Maybe<String> findNameByTaskId(Long taskId);

  Single<List<CohortOwnerInfo>> findAllCohortsWithNoTasks(Integer days);

  Single<List<String>> findInactiveCohorts(
      List<String> cohortNames, Integer inactiveSince, Integer limit);
}
