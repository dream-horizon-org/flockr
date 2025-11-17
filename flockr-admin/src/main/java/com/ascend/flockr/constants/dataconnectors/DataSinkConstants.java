package com.ascend.flockr.constants.dataconnectors;

import lombok.experimental.UtilityClass;

/**
 * Constants class containing column names for the data_sinks table.
 *
 * <p>This class provides static final constants for all column names in the data_sinks table,
 * ensuring type-safe references to database columns throughout the application.
 *
 * @author Flockr Team
 * @since 1.0
 */
@UtilityClass
public final class DataSinkConstants {
  /** Primary key column name. */
  public static final String ID = "id";

  /** Data sink name column name. */
  public static final String NAME = "name";

  /** Data connector type foreign key column name. */
  public static final String TYPE_ID = "type_id";

  /** Configuration JSON column name. */
  public static final String CONFIG = "config";

  /** Status column name. */
  public static final String STATUS = "status";

  /** Created by user column name. */
  public static final String CREATED_BY = "created_by";

  /** Created timestamp column name. */
  public static final String CREATED_AT = "created_at";

  /** Updated timestamp column name. */
  public static final String UPDATED_AT = "updated_at";
}
