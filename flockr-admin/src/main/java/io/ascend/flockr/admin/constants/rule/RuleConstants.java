package io.ascend.flockr.admin.constants.rule;

import lombok.experimental.UtilityClass;

/**
 * Constants class containing column names for the rules table.
 *
 * <p>This class provides static final constants for all column names in the rules table, ensuring
 * type-safe references to database columns throughout the application.
 *
 * @since 1.0
 */
@UtilityClass
public final class RuleConstants {
  /** Primary key column name. */
  public static final String ID = "id";

  /** Audience identifier foreign key column name. */
  public static final String AUDIENCE_ID = "audience_id";

  /** Tenant identifier column name. */
  public static final String TENANT_ID = "tenant_id";

  /** Project identifier column name. */
  public static final String PROJECT_ID = "project_id";

  /** Rule name column name. */
  public static final String NAME = "name";

  /** Rule description column name. */
  public static final String DESCRIPTION = "description";

  /** Rule start time column name. */
  public static final String START_TIME = "start_time";

  /** Rule end time column name. */
  public static final String END_TIME = "end_time";

  /** Rule action column name (ADD/REMOVE). */
  public static final String RULE_ACTION = "rule_action";

  /** Rule type column name (STREAM/BATCH). */
  public static final String RULE_TYPE = "rule_type";

  /** Rule status column name. */
  public static final String STATUS = "status";

  /** Rule configuration JSON column name. */
  public static final String CONFIGURATION = "configuration";

  /** Created by user column name. */
  public static final String CREATED_BY = "created_by";

  /** Created timestamp column name. */
  public static final String CREATED_AT = "created_at";

  /** Updated timestamp column name. */
  public static final String UPDATED_AT = "updated_at";
}
