package com.ascend.flockr.service;

import com.ascend.flockr.io.request.CreateAudienceRequest;
import com.ascend.flockr.io.request.CreateRulesRequest;
import com.ascend.flockr.io.response.AudienceDetailsResponse;
import com.ascend.flockr.io.response.RuleDetailsResponse;
import io.reactivex.rxjava3.core.Single;

public interface AudienceService {
  Single<Long> createAudience(CreateAudienceRequest request);

  Single<AudienceDetailsResponse> getAudienceDetails(Long audienceId);

  Single<Boolean> createRules(CreateRulesRequest request);

  Single<RuleDetailsResponse> getRuleDetails(Long audienceId, Long ruleId);
}
