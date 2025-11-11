package com.ascend.flockr.service.impl;

import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.io.request.CreateAudienceRequest;
import com.ascend.flockr.repository.AudienceRepository;
import com.ascend.flockr.service.AudienceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AudienceServiceImpl implements AudienceService {
  private final AudienceRepository audienceRepository;
  private final ObjectMapper objectMapper;

  @Override
  public Single<Long> createAudience(CreateAudienceRequest request) {
    String tenantId = "default-tenant";
    String projectId = "default-project";

    AudienceMeta audienceMeta =
        AudienceMeta.builder()
            .tenantId(tenantId)
            .projectId(projectId)
            .name(request.getName())
            .description(request.getDescription())
            .customAudienceConfig(request.getCustomAudienceConfig())
            .type(request.getType())
            .expireDate(request.getExpiryDate())
            .sinks(request.getSinkIds())
            .build();

    return audienceRepository.createAudience(audienceMeta);
  }

  @Override
  public Single<List<Long>> getSinkIdsByAudienceId(Long audienceId) {
    return null;
  }
}
