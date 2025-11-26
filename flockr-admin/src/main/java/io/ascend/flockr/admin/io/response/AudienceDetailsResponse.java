package io.ascend.flockr.admin.io.response;

import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.domain.dataconnectors.DataSinkDetails;
import com.ascend.flockr.domain.rule.RuleMeta;
import com.ascend.flockr.domain.rule.SourceInfoEnriched;
import java.util.List;

/**
 * @param sinks Associated sinks with metadata
 * @param rules Associated rules with enriched configuration
 */
public record AudienceDetailsResponse(
    AudienceMeta audienceMeta,
    List<DataSinkDetails> sinks,
    List<RuleMeta<SourceInfoEnriched>> rules) {}
