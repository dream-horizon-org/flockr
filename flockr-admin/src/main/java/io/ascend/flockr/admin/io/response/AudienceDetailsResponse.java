package io.ascend.flockr.admin.io.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.rule.RuleMeta;
import io.ascend.flockr.admin.domain.rule.SourceInfoEnriched;
import java.util.List;

/**
 * Response containing detailed information about an audience.
 *
 * <p>This response provides comprehensive audience data including:
 *
 * <ul>
 *   <li>The audience metadata (name, description, type, status, etc.)
 *   <li>All associated data sinks with full configuration details
 *   <li>All rules with enriched source information (including data source metadata)
 * </ul>
 *
 * @param audienceMeta the audience metadata
 * @param sinks list of associated data sinks with full details
 * @param rules list of associated rules with enriched source information
 * @since 1.0
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AudienceDetailsResponse(
    AudienceMeta audienceMeta,
    List<DataSinkDetails> sinks,
    List<RuleMeta<SourceInfoEnriched>> rules) {}
