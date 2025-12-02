package io.ascend.flockr.admin.io.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object containing basic audience metadata including rule count.
 *
 * <p>This response is used for listing audiences with essential information and is optimized for
 * list views where full details are not needed.
 *
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AudienceMetaResponse {
  /** The unique identifier of the audience. */
  private Long audienceId;

  /** The name of the audience. */
  private String name;

  /** A description of the audience's purpose or characteristics. */
  private String description;

  /** The type of audience (e.g., "custom", "cohort"). */
  private String type;

  /** Whether the audience has been verified. */
  private Boolean verified;

  /** The number of users currently in this audience. */
  private Long userCount;

  /** The total number of rules associated with this audience. */
  private Long ruleCount;

  /** The expiry date of the audience (epoch seconds). */
  private Long expireDate;

  /** Timestamp when the audience was created (epoch seconds). */
  private Long createdAt;

  /** Timestamp when the audience was last updated (epoch seconds). */
  private Long updatedAt;

  /** The username of the user who created this audience. */
  private String createdBy;
}
