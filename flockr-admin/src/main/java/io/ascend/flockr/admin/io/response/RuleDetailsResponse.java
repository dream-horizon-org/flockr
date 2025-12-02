package io.ascend.flockr.admin.io.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.ascend.flockr.admin.domain.rule.RuleMeta;
import io.ascend.flockr.admin.domain.rule.SourceInfoEnriched;

/**
 * Response containing detailed information about a single rule.
 *
 * <p>The rule details include enriched source information with full data source metadata.
 *
 * @param ruleDetails the rule metadata with enriched source information
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleDetailsResponse(RuleMeta<SourceInfoEnriched> ruleDetails) {}
