package io.ascend.flockr.users.util;

import io.ascend.flockr.users.constants.Constants;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class providing common helper methods used across the Flockr platform.
 *
 * <p>This class contains static utility methods for:
 *
 * <ul>
 *   <li>System information (CPU cores)
 *   <li>User key generation (userId/guestId handling)
 *   <li>Date/time parsing and epoch conversion
 * </ul>
 *
 * <p>This class cannot be instantiated. All methods are static.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
public class CommonUtils {
  /**
   * DateTimeFormatter instance configured with the standard date pattern used across the platform.
   *
   * <p>The pattern follows: {@code "yyyy-MM-dd HH:mm:ss"} as defined in {@link
   * Constants#DATE_PATTERN}.
   */
  @Getter
  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern(Constants.DATE_PATTERN);

  /**
   * Private constructor to prevent instantiation.
   *
   * @throws UnsupportedOperationException always thrown when constructor is invoked
   */
  private CommonUtils() {
    throw new UnsupportedOperationException("Constructor Invocation Unavailable for CommonUtils");
  }

  /**
   * Returns the number of available CPU cores on the system.
   *
   * <p>This method delegates to Vert.x's CPU core sensor to determine the number of processors
   * available to the JVM. This is useful for configuring thread pools and parallelism settings.
   *
   * @return the number of available CPU cores
   */
  public static Integer getNumOfCores() {
    return CpuCoreSensor.availableProcessors();
  }
}
