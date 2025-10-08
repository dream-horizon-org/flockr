package com.ascend.flockr.util;

import com.ascend.flockr.constants.Constants;
import com.ascend.flockr.constants.datadog.DDConstants;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class CommonUtil {

  public static int getNumberOfCores() {
    return CpuCoreSensor.availableProcessors();
  }

    public static String getCircuitBreakerAspect(String aspect) {
    return DDConstants.CB_METRIC + Constants.SPACE + aspect;
  }

  public static String getCircuitBreakerTag(String circuitBreakerName) {
    return DDConstants.CB_NAME + Constants.COLON + circuitBreakerName;
  }
  }
