package io.ascend.flockr.admin.io.response;

import com.ascend.flockr.domain.rule.RuleMeta;
import com.ascend.flockr.domain.rule.SourceInfoEnriched;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleDetailsResponse(RuleMeta<SourceInfoEnriched> ruleDetails) {}
