package io.ascend.flockr.users.constants;

/**
 * Centralized constants used across the Flockr platform.
 *
 * <p>This class contains application-wide constants including:
 *
 * <ul>
 *   <li>Configuration limits (max verticles)
 *   <li>Date/time patterns
 *   <li>Action identifiers
 *   <li>Source identifiers
 *   <li>Key prefixes
 * </ul>
 *
 * <p>This class cannot be instantiated. All constants are public static final.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
public class Constants {
  /**
   * Maximum number of REST verticles that can be deployed.
   *
   * <p>This constant limits the number of REST API verticles that can be instantiated in the
   * application. Used for resource management and load balancing.
   */
  public static final Integer MAX_NUM_REST_VERTICLES = 16;

  /**
   * Standard date-time pattern used throughout the platform.
   *
   * <p>Format: {@code "yyyy-MM-dd HH:mm:ss"}
   *
   * <p>Example: {@code "2024-01-15 14:30:00"}
   *
   * <p>This pattern is used by {@link io.ascend.flockr.users.util.CommonUtils#getFormatter()} for
   * parsing and formatting dates.
   */
  public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

  /**
   * Action identifier for append operations.
   *
   * <p>Used when adding or appending data (e.g., adding a user to a cohort).
   */
  public static final String ACTION_APPEND = "append";

  /**
   * Action identifier for remove operations.
   *
   * <p>Used when removing or deleting data (e.g., removing a user from a cohort).
   */
  public static final String ACTION_REMOVE = "remove";

  /**
   * Source identifier for Dream11 platform.
   *
   * <p>This is the default source identifier. When used with {@link
   * io.ascend.flockr.users.util.CommonUtils#getAerospikeSetNameFromSource(String, String)}, it does
   * not modify the base set name.
   */
  public static final String SOURCE_DREAM11 = "Dream11";

  /**
   * Source identifier for FanCode platform.
   *
   * <p>When used with {@link
   * io.ascend.flockr.users.util.CommonUtils#getAerospikeSetNameFromSource(String, String)}, it
   * appends "-fancode" to the base set name.
   */
  public static final String SOURCE_FANCODE = "FanCode";

  /**
   * Private constructor to prevent instantiation.
   *
   * @throws UnsupportedOperationException always thrown when constructor is invoked
   */
  private Constants() {
    throw new UnsupportedOperationException("Constructor Invocation Unavailable for Constants");
  }
}
