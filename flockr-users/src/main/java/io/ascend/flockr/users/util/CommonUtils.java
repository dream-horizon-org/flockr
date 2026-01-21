package io.ascend.flockr.users.util;

import com.dream11.rest.util.ExceptionUtil;
import io.ascend.flockr.users.constants.Constants;
import io.ascend.flockr.users.exception.errors.DefinedErrors;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
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

  /**
   * Generates a user key from either a user ID or guest ID.
   *
   * <p>This method prioritizes userId over guestId. If userId is not null, it returns the string
   * representation of userId. Otherwise, it returns the guestId.
   *
   * <p><strong>Usage:</strong>
   *
   * <pre>{@code
   * String key = CommonUtils.getUserKey(12345L, "guest-abc");
   * // Returns "12345"
   *
   * String key = CommonUtils.getUserKey(null, "guest-abc");
   * // Returns "guest-abc"
   * }</pre>
   *
   * @param userId the user ID, may be null
   * @param guestId the guest ID, used when userId is null
   * @return the user key string (userId if not null, otherwise guestId)
   */
  public static String getUserKey(Long userId, String guestId) {
    return Objects.nonNull(userId) ? userId.toString() : guestId;
  }

  /**
   * Converts an expiry date string to epoch milliseconds, with validation.
   *
   * <p>This method parses the expiry date string using the standard date format and converts it to
   * epoch milliseconds (UTC). It validates that the expiry time is not in the past.
   *
   * <p><strong>Validation Rules:</strong>
   *
   * <ul>
   *   <li>If action is {@link Constants#ACTION_APPEND} and expiry is in the past, throws {@link
   *       DefinedErrors#INVALID_EXPIRY_TIME}
   *   <li>If action is {@link Constants#ACTION_REMOVE} and expiry is in the past, returns 0L
   * </ul>
   *
   * <p><strong>Date Format:</strong> The expiry date must follow the pattern {@code "yyyy-MM-dd
   * HH:mm:ss"} as defined in {@link Constants#DATE_PATTERN}.
   *
   * @param expireAt the expiry date string in format "yyyy-MM-dd HH:mm:ss"
   * @param action the action being performed ("append" or "remove")
   * @return the expiry time as epoch milliseconds, or 0L if action is "remove" and expiry is
   *     invalid
   * @throws RuntimeException if action is "append" and expiry time is in the past
   */
  public static long getEpochFromExpireAt(String expireAt, String action) {
    long expiryEpoch =
        LocalDateTime.parse(expireAt, formatter).toEpochSecond(ZoneOffset.UTC) * 1000L;
    long currentTime = System.currentTimeMillis();
    if (expiryEpoch < currentTime) {
      if (action.equals(Constants.ACTION_APPEND)) {
        log.error("Invalid expiryAt: {}", expireAt);
        throw ExceptionUtil.getException(DefinedErrors.INVALID_EXPIRY_TIME, expireAt);
      } else return 0L;
    }
    return expiryEpoch;
  }
}
