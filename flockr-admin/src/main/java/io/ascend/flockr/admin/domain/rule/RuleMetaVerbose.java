package io.ascend.flockr.admin.domain.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleMetaVerbose<T extends SourceInfo, V extends SinkInfo> extends RuleMeta<T> {
  private String audienceName;
  private List<V> sinkList;
}
