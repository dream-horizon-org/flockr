package io.ascend.flockr.engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single audience update request item.
 *
 * <p>This DTO (Data Transfer Object) is used to serialize audience update requests to JSON. Each
 * request contains information about a user's association with an audience.
 *
 * <p><b>Example JSON:</b>
 *
 * <pre>{@code
 * {
 *   "user_id": "12345",
 *   "audience_key": "audience-1",
 *   "action": "append",
 *   "expire_at": 1735689599
 * }
 * }</pre>
 *
 * <p><b>Factory Methods:</b>
 *
 * <p>To create instances of this DTO, use {@link
 * io.ascend.flockr.engine.dto.builder.AudienceUpdateRequestFactory}. The factory provides methods
 * for creating requests with different user ID types.
 *
 * @see io.ascend.flockr.engine.dto.builder.AudienceUpdateRequestFactory
 * @author Shivam-Raghuwanshi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudienceUpdateRequest {

  /** The user ID. Can be a number or string depending on the user ID format. */
  @JsonProperty("user_id")
  private Object userId;

  /** The audience key (name) - maps to audienceName from application arguments. */
  @JsonProperty("audience_key")
  private String audienceKey;

  /** The action to perform ("append" or "remove"). */
  @JsonProperty("action")
  private String action;

  /** The expiration timestamp in epoch seconds. */
  @JsonProperty("expire_at")
  private Long expireAt;
}
