package com.ascend.flockr.common.client;

import io.reactivex.rxjava3.core.Single;
import java.util.Map;

public interface Aerospike {
  Single<Boolean> isConnected();

  Single<Map<String, Long>> getCohortExpiryBin(String id, String setName);

  Single<Boolean> appendCohort(String id, String cohort, String source, Long cohortExpiry);

  Single<Boolean> removeCohort(String id, String cohort, String source);
}
