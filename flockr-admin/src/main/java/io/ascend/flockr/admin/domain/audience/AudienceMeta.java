package io.ascend.flockr.admin.domain.audience;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.json.JsonObject;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing audience metadata.
 *
 * <p>An audience represents a group of users defined by rules and configurations. This class
 * contains all metadata associated with an audience, including tenant/project identifiers, name,
 * description, type, custom configuration, associated data sinks, verification status, and
 * timestamps.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudienceMeta {
  /** The tenant identifier that owns this audience. */
  private String tenantId;

  /** The project identifier within the tenant. */
  private String projectId;

  /** The unique identifier of the audience. */
  private Long audienceId;

  /** The name of the audience. */
  private String name;

  /** A description of the audience's purpose or characteristics. */
  private String description;

  /** The type of audience (e.g., "custom", "cohort"). */
  private String type;

  /** Custom configuration for the audience as a JSON object. */
  private JsonObject customAudienceConfig;

  /** List of data sink identifiers associated with this audience. */
  private List<Long> sinks;

  /** Whether the audience has been verified. */
  private Boolean verified;

  /** The number of users currently in this audience. */
  private Long userCount;

  /** Expiry date of the audience as epoch milliseconds. */
  private Long expireDate;

  /** Timestamp of the last audience update as epoch milliseconds. */
  private Long lastAudienceUpdatedAt;

  /** Timestamp when the audience was created as epoch milliseconds. */
  private Long createdAt;

  /** Timestamp when the audience was last updated as epoch milliseconds. */
  private Long updatedAt;

  /** Username of the user who created this audience. */
  private String createdBy;
}
