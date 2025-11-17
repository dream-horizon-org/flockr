package com.ascend.flockr.repository;

import com.ascend.flockr.domain.rule.RuleMeta;
import com.ascend.flockr.domain.rule.SourceInfoBasic;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface RuleRepository {
  Single<Boolean> createRules(List<RuleMeta<SourceInfoBasic>> ruleMetas);

  Single<RuleMeta<SourceInfoBasic>> getRuleById(String tenantId, String projectId, Long ruleId);

  Single<List<RuleMeta<SourceInfoBasic>>> getRulesByAudienceId(
      String tenantId, String projectId, Long audienceId);
}
