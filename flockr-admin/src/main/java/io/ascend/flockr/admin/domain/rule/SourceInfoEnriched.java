package io.ascend.flockr.admin.domain.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Domain model representing enriched source information.
 *
 * <p>This class extends {@link SourceInfo} to include the full {@link DataSourceDetails} for a data
 * source. It is used in API responses where clients need complete source metadata, such as
 * connection details, type information, and configuration.
 *
 * <p>The enrichment process typically involves:
 *
 * <ol>
 *   <li>Fetching basic rule configuration with {@link SourceInfo} references
 *   <li>Looking up full {@link DataSourceDetails} for each referenced source
 *   <li>Building {@link SourceInfoEnriched} instances with the combined data
 * </ol>
 *
 * @since 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SourceInfoEnriched extends SourceInfo {
  /** The complete data source details including name, type, config, and status. */
  private DataSourceDetails details;
}
