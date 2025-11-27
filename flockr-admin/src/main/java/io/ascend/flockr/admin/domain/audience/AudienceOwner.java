package io.ascend.flockr.admin.domain.audience;

import static io.ascend.flockr.admin.constants.audience.AudienceOwnersConstants.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.rxjava3.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing an audience owner.
 *
 * <p>An audience owner is a user who has permissions to manage an audience, including the ability
 * to add or remove other owners. This class contains all metadata associated with an owner record.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudienceOwner {
  /** Unique identifier for the owner record. */
  private Long id;

  /** The audience identifier this owner belongs to. */
  private Long audienceId;

  /** Email address of the owner. */
  private String ownerEmail;

  /** Current status of the owner (ACTIVE, INACTIVE). */
  private String status;

  /** Timestamp when the owner was added (epoch seconds). */
  private Long createdAt;

  /** Timestamp when the owner was last updated (epoch seconds). */
  private Long updatedAt;

  /**
   * Maps a database row to an AudienceOwner domain object.
   *
   * @param row the database row containing owner data
   * @return an AudienceOwner instance with data from the row
   */
  public static AudienceOwner mapOwnerRow(Row row) {
    return AudienceOwner.builder()
        .id(row.getLong(ID))
        .audienceId(row.getLong(AUDIENCE_ID))
        .ownerEmail(row.getString(OWNER_EMAIL))
        .status(row.getString(STATUS))
        .createdAt(row.getLong(CREATED_AT))
        .updatedAt(row.getLong(UPDATED_AT))
        .build();
  }
}
