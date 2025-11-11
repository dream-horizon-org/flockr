package com.ascend.flockr.repository;

import com.ascend.flockr.domain.audience.AudienceMeta;
import io.reactivex.rxjava3.core.Single;

public interface AudienceRepository {
  Single<Long> createAudience(AudienceMeta meta);

  Single<Long> addOwner(String email, Long audienceId);

  Single<AudienceMeta> getAudienceById(Long id);
}
