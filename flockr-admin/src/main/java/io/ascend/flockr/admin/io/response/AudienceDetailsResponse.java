package io.ascend.flockr.admin.io.response;

import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.rule.RuleMeta;
import io.ascend.flockr.admin.domain.rule.SourceInfoEnriched;
import java.util.List;

/**
 * @param sinks Associated sinks with metadata
 * @param rules Associated rules with enriched configuration
 */
public record AudienceDetailsResponse(
    AudienceMeta audienceMeta,
    List<DataSinkDetails> sinks,
    List<RuleMeta<SourceInfoEnriched>> rules) {}
