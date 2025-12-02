package io.ascend.flockr.admin.io.request;

import io.vertx.core.json.JsonObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for creating a new audience.
 *
 * <p>An audience represents a group of users defined by rules. This request provides all the
 * necessary metadata and configuration for creating an audience.
 *
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAudienceRequest {
  /** The name of the audience. Must not be empty. */
  @NotEmpty private String name;

  /** A description of the audience's purpose or characteristics. Must not be empty. */
  @NotEmpty private String description;

  /** Optional custom configuration for the audience as a JSON object. */
  private JsonObject customAudienceConfig;

  /** The type of audience (e.g., "custom", "cohort"). Must not be empty. */
  @NotEmpty private String type;

  /**
   * The expiry date of the audience (epoch milliseconds). Must not exceed year 2039 to prevent
   * overflow issues.
   */
  @Max(2177452799000L)
  @NotNull
  private Long expireDate;

  /** List of data sink IDs to associate with this audience. Must contain at least one sink. */
  @Valid @NotEmpty private List<Long> sinkIds;
}
