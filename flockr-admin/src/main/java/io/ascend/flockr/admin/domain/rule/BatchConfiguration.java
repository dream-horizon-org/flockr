package io.ascend.flockr.admin.domain.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.ascend.flockr.admin.validation.ValidSqlQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties()
public class BatchConfiguration<T extends SourceInfo> implements RuleConfiguration<T> {
  @Valid
  @NotNull(message = "Source is required")
  private T source;

  @NotEmpty @ValidSqlQuery private String query;
  private String cronExpression;
}
