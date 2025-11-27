package io.ascend.flockr.admin.util;

import io.ascend.flockr.admin.constants.Constants;
import io.ascend.flockr.admin.constants.datadog.DDConstants;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import lombok.experimental.UtilityClass;

/**
 * Utility class providing common helper methods used throughout the application.
 *
 * <p>This class provides utilities for:
 *
 * <ul>
 *   <li>System resource detection (CPU cores)
 *   <li>Circuit breaker metric and tag formatting
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
@UtilityClass
public final class CommonUtil {

  /**
   * Gets the number of available CPU cores on the system.
   *
   * @return the number of available processors
   */
  public static int getNumberOfCores() {
    return CpuCoreSensor.availableProcessors();
  }

  /**
   * Constructs a circuit breaker metric aspect string.
   *
   * @param aspect the aspect name to append
   * @return formatted metric aspect string
   */
  public static String getCircuitBreakerAspect(String aspect) {
    return DDConstants.CB_METRIC + Constants.SPACE + aspect;
  }

  /**
   * Constructs a circuit breaker tag string.
   *
   * @param circuitBreakerName the circuit breaker name
   * @return formatted tag string
   */
  public static String getCircuitBreakerTag(String circuitBreakerName) {
    return DDConstants.CB_NAME + Constants.COLON + circuitBreakerName;
  }
}
