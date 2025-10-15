package com.ascend.flockr.users.service.impl;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import com.ascend.flockr.users.service.UserCohortsService;
import com.dream11.rest.exception.RestException;
import com.dream11.rest.util.ExceptionUtil;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Map;

public class UserCohortServiceImpl implements UserCohortsService {
  private final Aerospike aerospikeClient;

  private final AerospikeConfig aerospikeConfig;

  @Override
  public Single<List<String>> getCohorts(Long userId, String guestId, String source) {
    String userKey = CommonUtils.getUserKey(userId, guestId);
    return aerospikeClient.getCohortExpiryBin(userKey, source).map(this::getActiveCohortsFromMap);
  }

  @Override
  public Single<Boolean> mapUserCohorts(MapUserCohortsRequest request) {
    String userKey = CommonUtils.getUserKey(request.getUserId(), request.getGuestId());
    String source = request.getSource();

    Single<Boolean> single;
    try {
      if (request.getAction().equals(Constants.ACTION_APPEND)) {
        Long cohortExpiry = request.expiryEpochFromExpireAt();
        single =
            aerospikeClient.appendCohort(userKey, request.getCohortKey(), source, cohortExpiry);
      } else {
        single = aerospikeClient.removeCohort(userKey, request.getCohortKey(), source);
      }
    } catch (Exception e) {
      single = Single.error(e);
    }
    return single.onErrorResumeNext(
        throwable -> {
          if (throwable instanceof AerospikeException aerospikeException
              && aerospikeException.getResultCode() == ResultCode.KEY_NOT_FOUND_ERROR) {
            return Single.just(false);
          } else if (throwable instanceof RestException) {
            log.error("Invalid expiryAt: {}", throwable.getMessage());
            return Single.error(
                ExceptionUtil.getException(
                    DefinedErrors.INVALID_EXPIRY_TIME, throwable.getMessage()));
          } else {
            log.error("An internal server error occurred: {}", throwable.getMessage(), throwable);
            return Single.error(
                ExceptionUtil.getException(
                    DefinedErrors.INTERNAL_SERVER_ERROR, throwable.getMessage()));
          }
        });
  }

  private List<String> getActiveCohortsFromMap(Map<String, Long> cohortMap) {
    Long currentTime = System.currentTimeMillis();

    return cohortMap.entrySet().stream()
        .filter(cohortEntry -> cohortEntry.getValue() >= currentTime)
        .map(Map.Entry::getKey)
        .toList();
  }
}
