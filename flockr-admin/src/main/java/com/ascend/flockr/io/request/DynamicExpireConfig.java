package com.ascend.flockr.io.request;

import com.ascend.flockr.io.DynamicExpireTimeUnit;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DynamicExpireConfig {
  @NotNull private Long userValidityValue;
  @NotNull private DynamicExpireTimeUnit userValidityTimeUnit;
}
