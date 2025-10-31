package com.ascend.flockr.users.service;

import com.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface UserCohortsService {
  Single<List<String>> getCohorts(Long userId, String guestId, Long projectId);

  Single<Boolean> mapUserCohorts(MapUserCohortsRequest request);
}
