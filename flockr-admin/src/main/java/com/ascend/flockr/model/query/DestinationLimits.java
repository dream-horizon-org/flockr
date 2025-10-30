package com.ascend.flockr.model.query;

import com.ascend.flockr.exception.DefinedException;
import com.ascend.flockr.exception.ErrorEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DestinationLimits {

  public static final Long unitTenMillions = 10000000L;
  private Map<String, DestinationProperties> destinationLimits;

  public void checkLimitWithinBounds(
      List<String> selectedDestinations, Long queryLimit, Optional<String> ruleName) {
    for (String dest : selectedDestinations) {
      DestinationProperties destLimit = destinationLimits.get(dest);
      if (destLimit == null) {
        continue;
      }
      long limitConvertedToMillion = (long) (destLimit.getLimit() * unitTenMillions);

      if (limitConvertedToMillion < queryLimit) {
        if (ruleName.isPresent()) {
          throw new DefinedException(
              ErrorEntity.DESTINATION_LIMIT_EXCEEDED_RULE_QUERY_VALIDATION,
              ruleName.get(),
              destLimit.getLimit());
        } else {
          throw new DefinedException(
              ErrorEntity.DESTINATION_LIMIT_EXCEEDED_QUERY_VALIDATION,
              destLimit.getLimit(),
              destLimit.getAlias());
        }
      }
    }
  }

  public Long getMinLimit(List<String> selectedDestinations) {
    long minLimit = 80 * unitTenMillions;
    for (String dest : selectedDestinations) {
      if (destinationLimits.get(dest) == null) {
        continue;
      }

      Double destLimit = destinationLimits.get(dest).getLimit() * unitTenMillions;
      minLimit = Math.min(minLimit, destLimit.longValue());
    }
    return minLimit;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class DestinationProperties {
    private Double limit;
    private String alias;
  }
}
