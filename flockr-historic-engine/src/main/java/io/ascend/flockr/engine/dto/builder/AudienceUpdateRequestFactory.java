package io.ascend.flockr.engine.dto.builder;

import io.ascend.flockr.engine.dto.AudienceUpdateRequest;

/**
 * Factory class for creating {@link AudienceUpdateRequest} instances.
 *
 * <p>This factory provides methods to create audience update requests with different user ID types
 * (string or numeric). It handles automatic type conversion and default value assignment.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create from string user ID (auto-detects numeric)
 * AudienceUpdateRequest request = AudienceUpdateRequestFactory.fromString(
 *     "12345", "audience-1", "append", 1735689599L
 * );
 *
 * // Create with explicit string user ID
 * AudienceUpdateRequest request = AudienceUpdateRequestFactory.withStringUserId(
 *     "user-123", "audience-1", "append", 1735689599L
 * );
 *
 * // Create with explicit numeric user ID
 * AudienceUpdateRequest request = AudienceUpdateRequestFactory.withNumericUserId(
 *     12345L, "audience-1", "append", 1735689599L
 * );
 * }</pre>
 *
 * <p><b>Future Extensibility:</b>
 *
 * <p>This factory can be extended to support additional API request types as needed. For example,
 * if new request types are added (e.g., AudienceDeleteRequest, AudienceQueryRequest), similar
 * factory methods can be added here or in separate factory classes following the same pattern.
 *
 * @see AudienceUpdateRequest
 * @author Shivam-Raghuwanshi
 */
public class AudienceUpdateRequestFactory {

  /**
   * Creates an AudienceUpdateRequest with a string user ID.
   *
   * @param userId The user ID as a string.
   * @param audienceName The audience name (maps to audience_key).
   * @param action The action ("append" or "remove").
   * @param expireAt The expiration timestamp in epoch seconds.
   * @return A new AudienceUpdateRequest instance.
   */
  public static AudienceUpdateRequest withStringUserId(
      String userId, String audienceName, String action, Long expireAt) {
    return AudienceUpdateRequest.builder()
        .userId(userId)
        .audienceKey(audienceName)
        .action(action != null ? action : "append")
        .expireAt(expireAt != null ? expireAt : 0L)
        .build();
  }

  /**
   * Creates an AudienceUpdateRequest with a numeric user ID.
   *
   * @param userId The user ID as a long.
   * @param audienceName The audience name (maps to audience_key).
   * @param action The action ("append" or "remove").
   * @param expireAt The expiration timestamp in epoch seconds.
   * @return A new AudienceUpdateRequest instance.
   */
  public static AudienceUpdateRequest withNumericUserId(
      Long userId, String audienceName, String action, Long expireAt) {
    return AudienceUpdateRequest.builder()
        .userId(userId)
        .audienceKey(audienceName)
        .action(action != null ? action : "append")
        .expireAt(expireAt != null ? expireAt : 0L)
        .build();
  }

  /**
   * Creates an AudienceUpdateRequest from a string user ID, automatically converting to numeric if
   * possible.
   *
   * <p>This method attempts to parse the string as a long. If successful, it creates a request with
   * a numeric user ID. Otherwise, it creates a request with a string user ID.
   *
   * @param userIdStr The user ID as a string.
   * @param audienceName The audience name (maps to audience_key) - comes from application
   *     arguments.
   * @param action The action ("append" or "remove").
   * @param expireAt The expiration timestamp in epoch seconds.
   * @return A new AudienceUpdateRequest instance with numeric or string user ID.
   */
  public static AudienceUpdateRequest fromString(
      String userIdStr, String audienceName, String action, Long expireAt) {
    try {
      Long userId = Long.parseLong(userIdStr);
      return withNumericUserId(userId, audienceName, action, expireAt);
    } catch (NumberFormatException e) {
      return withStringUserId(userIdStr, audienceName, action, expireAt);
    }
  }
}
