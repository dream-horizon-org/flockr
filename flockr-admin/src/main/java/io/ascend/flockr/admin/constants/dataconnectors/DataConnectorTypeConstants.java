package io.ascend.flockr.admin.constants.dataconnectors;

import lombok.experimental.UtilityClass;

/**
 * Constants class containing column names for the data_connector_types table.
 *
 * <p>This class provides static final constants for all column names in the data_connector_types
 * table, ensuring type-safe references to database columns throughout the application.
 *
 * @since 1.0
 */
@UtilityClass
public final class DataConnectorTypeConstants {
  /** Primary key column name. */
  public static final String ID = "id";

  /** Connector kind column name (SOURCE/SINK). */
  public static final String KIND = "kind";

  /** Connector type column name. */
  public static final String TYPE = "type";

  /** Display name column name. */
  public static final String DISPLAY_NAME = "display_name";

  /** Configuration schema JSON column name. */
  public static final String CONFIG_SCHEMA = "config_schema";

  /** Active status column name. */
  public static final String IS_ACTIVE = "is_active";

  /** Created timestamp column name. */
  public static final String CREATED_AT = "created_at";

  /** Updated timestamp column name. */
  public static final String UPDATED_AT = "updated_at";
}
