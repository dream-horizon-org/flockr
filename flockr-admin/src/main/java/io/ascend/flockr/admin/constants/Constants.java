package io.ascend.flockr.admin.constants;

import lombok.experimental.UtilityClass;

/**
 * Utility class containing application-wide constants.
 *
 * <p>This class provides constants for:
 *
 * <ul>
 *   <li>Package names for component scanning
 *   <li>Application environment configuration keys
 *   <li>Deployment configuration limits
 *   <li>Common string delimiters
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
@UtilityClass
public final class Constants {
  /** Base package name for component scanning. */
  public static final String PACKAGE_NAME = "com.ascend.flockr";

  /** System property key for application environment. */
  public static final String APP_ENV_KEY = "app.environment";

  /** Default application environment value. */
  public static final String DEFAULT_APP_ENV = "dev";

  /** Maximum number of REST API verticle instances to deploy. */
  public static final Integer MAX_NUM_REST_VERTICLES = 1;

  /* Delimiter Constants */
  /** Comma delimiter string. */
  public static final String COMMA = ",";

  /** Colon delimiter string. */
  public static final String COLON = ":";

  /** Space delimiter string. */
  public static final String SPACE = " ";
}
