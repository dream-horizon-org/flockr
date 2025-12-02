package io.ascend.flockr.admin.domain.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Domain model representing basic source information.
 *
 * <p>This class contains the minimal information needed to identify a data source: its unique
 * identifier. It serves as the base class for {@link SourceInfoEnriched} which includes full source
 * details.
 *
 * <p>Used in rule configurations to reference data sources without embedding the full source
 * metadata, which reduces payload size and keeps configurations lightweight.
 *
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SourceInfo {
  /** The unique identifier of the data source. */
  private Long id;
}
