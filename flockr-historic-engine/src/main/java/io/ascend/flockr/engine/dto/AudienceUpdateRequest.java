package io.ascend.flockr.engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single audience update request item.
 *
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
  @JsonProperty("cohort_key")
  private String audienceKey;

  /** The action to perform ("append" or "remove"). */
  @JsonProperty("action")
  private String action;

  /** The expiration timestamp in epoch seconds. */
  @JsonProperty("expire_at")
  private Long expireAt;
}
