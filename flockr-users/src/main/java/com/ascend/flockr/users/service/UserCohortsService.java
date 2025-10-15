package com.ascend.flockr.users.service;

import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface UserCohortsService {
  Single<List<String>> getCohorts(Long userId, String guestId, String source);

  Single<Boolean> mapUserCohorts(MapUserCohortsRequest request);
}
