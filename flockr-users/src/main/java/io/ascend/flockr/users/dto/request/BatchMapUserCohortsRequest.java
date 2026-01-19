package io.ascend.flockr.users.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ascend.flockr.users.annotation.AcceptedValues;
import io.ascend.flockr.users.constants.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for a single user cohort mapping in a batch operation.
 *
 * <p>Each request in the batch contains:
 *
 * <ul>
 *   <li>User ID to map
 *   <li>Cohort key to assign/remove
 *   <li>Action type (append or remove)
 *   <li>Expiry time for append operations
 * </ul>
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Single user cohort mapping request for batch operations")
public class BatchMapUserCohortsRequest {

  @NotBlank
  @JsonProperty("user_id")
  @Schema(description = "User ID to map (must be positive)", example = "12345")
  private String userId;

  @NotBlank
  @JsonProperty("cohort_key")
  @Schema(description = "Cohort key/name to assign or remove user from", example = "premium-users")
  private String cohortKey;

  @NotBlank
  @AcceptedValues(values = {Constants.ACTION_APPEND, Constants.ACTION_REMOVE})
  @Schema(
      description = "Action to perform: 'append' to add user or 'remove' to remove user",
      example = "append",
      allowableValues = {"append", "remove"})
  private String action;

  @NotNull
  @JsonProperty("expire_at")
  @Schema(
      description =
          "Expiry time for the cohort membership as Unix epoch timestamp in milliseconds (required for append action)",
      example = "1735689599000")
  private Long expireAt;
}
