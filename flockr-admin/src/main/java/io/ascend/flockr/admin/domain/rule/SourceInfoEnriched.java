package io.ascend.flockr.admin.domain.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SourceInfoEnriched extends SourceInfo {
  private DataSourceDetails details;
}
