package io.ascend.flockr.admin.constants.audience;

import lombok.experimental.UtilityClass;

/**
 * Constants class containing column names for the audience_owners table.
 *
 * <p>This class provides static final constants for all column names in the audience_owners table,
 * ensuring type-safe references to database columns throughout the application.
 *
 * @since 1.0
 */
@UtilityClass
public final class AudienceOwnersConstants {
  /** Primary key column name. */
  public static final String ID = "id";

  /** Audience identifier foreign key column name. */
  public static final String AUDIENCE_ID = "audience_id";

  /** Encrypted project identifier column name (amalgamation of tenantId and projectId). */
  public static final String X_PROJECT_ID = "x_project_id";

  /** Owner email address column name. */
  public static final String OWNER_EMAIL = "owner_email";

  /** Status column name. */
  public static final String STATUS = "status";

  /** Created timestamp column name. */
  public static final String CREATED_AT = "created_at";

  /** Updated timestamp column name. */
  public static final String UPDATED_AT = "updated_at";
}
