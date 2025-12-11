package io.ascend.flockr.admin.util;

import io.vertx.core.impl.cpu.CpuCoreSensor;
import java.net.Inet4Address;
import java.net.UnknownHostException;
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

  public static String getHostAddress() {
    try {
      return Inet4Address.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      throw new RuntimeException("Failed to get host address", e);
    }
  }
}
