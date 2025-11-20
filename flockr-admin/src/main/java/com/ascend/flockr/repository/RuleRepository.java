package com.ascend.flockr.repository;

import com.ascend.flockr.domain.rule.RuleMeta;
import com.ascend.flockr.domain.rule.SourceInfo;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface RuleRepository {
  Single<Boolean> createRules(List<RuleMeta<SourceInfo>> ruleMetas);

  Single<RuleMeta<SourceInfo>> getRuleById(String tenantId, String projectId, Long ruleId);

  Single<List<RuleMeta<SourceInfo>>> getRulesByAudienceId(
      String tenantId, String projectId, Long audienceId);
}
