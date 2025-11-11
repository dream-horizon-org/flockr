package com.ascend.flockr.service;

import com.ascend.flockr.io.request.CreateAudienceRequest;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface AudienceService {
  Single<Long> createAudience(CreateAudienceRequest request);

  Single<List<Long>> getSinkIdsByAudienceId(Long audienceId);
}
