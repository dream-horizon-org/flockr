package io.ascend.flockr.admin.constants.audience;

import lombok.experimental.UtilityClass;

/**
 * Constants class containing column names for the audience table.
 *
 * <p>This class provides static final constants for all column names in the audience/audiences
 * table, ensuring type-safe references to database columns throughout the application.
 *
 * @since 1.0
 */
@UtilityClass
public final class AudienceConstants {
  /** Primary key column name. */
  public static final String ID = "id";

  /** Tenant identifier column name. */
  public static final String TENANT_ID = "tenant_id";

  /** Project identifier column name. */
  public static final String PROJECT_ID = "project_id";

  /** Audience name column name. */
  public static final String NAME = "name";

  /** Audience description column name. */
  public static final String DESCRIPTION = "description";

  /** Audience status column name. */
  public static final String STATUS = "status";

  /** Data sink IDs array column name. */
  public static final String SINKS = "sinks";

  /** Created by user column name. */
  public static final String CREATED_BY = "created_by";

  /** Created timestamp column name. */
  public static final String CREATED_AT = "created_at";

  /** Updated timestamp column name. */
  public static final String UPDATED_AT = "updated_at";

  /** Last audience update timestamp column name. */
  public static final String LAST_AUDIENCE_UPDATED_AT = "last_audience_updated_at";

  /** User count column name. */
  public static final String USER_COUNT = "user_count";

  /** Custom audience configuration JSON column name. */
  public static final String CUSTOM_AUDIENCE_CONFIG = "custom_audience_config";

  /** Audience type column name. */
  public static final String TYPE = "type";

  /** Verified status column name. */
  public static final String VERIFIED = "verified";

  /** Rules count column name. */
  public static final String RULES_COUNT = "rules_count";

  /** Expiry date column name. */
  public static final String EXPIRY_DATE = "expire_date";

  /** Expire date column name (PostgreSQL variant). */
  public static final String EXPIRE_DATE = "expire_date";

  /** Name vector column name (PostgreSQL full-text search). */
  public static final String NAME_VECTOR = "name_vector";
}
