package io.ascend.flockr.admin.domain.audience;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing a single audience record to be pushed to sinks.
 *
 * <p>This is the standardized format used across all sink types (Kafka, S3, Webhook).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudienceRecord {

  /** The name of the audience. */
  private String audienceName;

  /** The unique user identifier. */
  private String userId;

  /** The expiry date of the audience membership (epoch milliseconds). */
  private Long expireDate;

  /** The action to perform: "add" or "remove". */
  private String action;

  /** Converts this record to a JsonObject for serialization. */
  public JsonObject toJson() {
    return new JsonObject()
        .put("audienceName", audienceName)
        .put("userId", userId)
        .put("expireDate", expireDate)
        .put("action", action);
  }

  /** Creates an AudienceRecord from audience metadata, userId, and action. */
  public static AudienceRecord fromAudience(AudienceMeta audience, String userId, String action) {
    return AudienceRecord.builder()
        .audienceName(audience.getName())
        .userId(userId)
        .expireDate(audience.getExpireDate())
        .action(action)
        .build();
  }
}
