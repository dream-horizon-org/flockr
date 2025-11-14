package com.ascend.flockr.io.response;

import com.ascend.flockr.domain.rule.RuleMeta;
import com.ascend.flockr.domain.rule.SourceInfoEnriched;

public record RuleDetailsResponse(RuleMeta<SourceInfoEnriched> ruleDetails) {}
