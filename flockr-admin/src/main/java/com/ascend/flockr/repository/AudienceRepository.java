package com.ascend.flockr.repository;

import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.io.response.AudienceMetaResponse;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface AudienceRepository {
  Single<Long> createAudience(AudienceMeta meta);

  Single<AudienceMeta> getAudienceById(String tenantId, String projectId, Long id);

  Single<List<AudienceMetaResponse>> getAudiencesList(
      String tenantId,
      String projectId,
      String nameSearch,
      String createdBy,
      Boolean verified,
      Integer limit,
      Integer offset);
}
