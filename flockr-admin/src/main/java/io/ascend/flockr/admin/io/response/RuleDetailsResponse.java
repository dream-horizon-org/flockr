package io.ascend.flockr.admin.io.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.ascend.flockr.admin.domain.rule.RuleMeta;
import io.ascend.flockr.admin.domain.rule.SourceInfoEnriched;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleDetailsResponse(RuleMeta<SourceInfoEnriched> ruleDetails) {}
