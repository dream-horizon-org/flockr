package io.ascend.flockr.admin.io.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object containing audience owner information.
 *
 * <p>Used for representing individual audience owners with their associated metadata.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AudienceOwnerResponse {
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
}
