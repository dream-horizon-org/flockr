package io.ascend.flockr.users.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ascend.flockr.users.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class BatchMapUserCohortsRequest {

  @NotNull
  @Positive
  @JsonProperty("user_id")
  private Long userId;

  @NotBlank
  @JsonProperty("cohort_key")
  private String cohortKey;

  @NotBlank private String action;

  @NotBlank
  @DateTimeFormat
  @JsonProperty("expire_at")
  private String expireAt;
}
