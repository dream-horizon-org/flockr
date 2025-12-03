package io.ascend.flockr.admin.io.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request object for adding or removing an audience owner.
 *
 * @since 1.0
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateAudienceOwnerRequest {

  /** The action to perform (ADD or REMOVE). */
  @NotNull private UpdateAudienceOwnerAction action;

  /** The email address of the owner to add or remove. */
  @NotEmpty private String email;
}
