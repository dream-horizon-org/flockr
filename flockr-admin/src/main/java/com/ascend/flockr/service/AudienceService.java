package com.ascend.flockr.service;

import com.ascend.flockr.io.request.CreateAudienceRequest;
import com.ascend.flockr.io.request.CreateRulesRequest;
import com.ascend.flockr.io.response.AudienceDetailsResponse;
import com.ascend.flockr.io.response.AudienceMetaResponse;
import com.ascend.flockr.io.response.RuleDetailsResponse;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface AudienceService {
  Single<Long> createAudience(String tenantId, String projectId, CreateAudienceRequest request);

  Single<AudienceDetailsResponse> getAudienceDetails(
      String tenantId, String projectId, Long audienceId);

  Single<Boolean> createRules(String tenantId, String projectId, CreateRulesRequest request);

  Single<RuleDetailsResponse> getRuleDetails(
      String tenantId, String projectId, Long audienceId, Long ruleId);

  Single<List<AudienceMetaResponse>> getAudiencesList(
      String tenantId,
      String projectId,
      String nameSearch,
      String createdBy,
      Boolean verified,
      Integer limit,
      Integer offset);
}
