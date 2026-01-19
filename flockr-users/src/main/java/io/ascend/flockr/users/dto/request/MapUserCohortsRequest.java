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
 * Request DTO for mapping a user to a cohort.
 *
 * <p>Contains all information needed to assign or remove a user from a cohort, including cohort
 * name, action type, and expiry information.
 *
 * <p>Note: userId, tenantId, and projectId are now passed via headers, not in the request body. API
 * fields use snake_case naming convention.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request to map a user to a cohort")
public class MapUserCohortsRequest {
  /** Cohort name to assign/remove user from (snake_case for API). */
  @NotBlank
  @JsonProperty("cohort_key")
  @Schema(description = "Cohort key/name to assign or remove user from", example = "premium-users")
  private String cohortKey;

  /** Action type: "append" to add user, "remove" to remove user. */
  @NotBlank
  @AcceptedValues(values = {Constants.ACTION_APPEND, Constants.ACTION_REMOVE})
  @Schema(
      description = "Action to perform: 'append' to add user or 'remove' to remove user",
      example = "append",
      allowableValues = {"append", "remove"})
  private String action;

  /** Expiry time as Unix epoch timestamp in milliseconds (required for append action). */
  @NotNull
  @JsonProperty("expire_at")
  @Schema(
      description =
          "Expiry time for the cohort membership as Unix epoch timestamp in milliseconds (required for append action)",
      example = "1735689599000")
  private Long expireAt;
}
