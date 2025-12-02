package io.ascend.flockr.admin.io.request;

import io.ascend.flockr.admin.domain.audience.AudienceType;
import io.ascend.flockr.admin.validation.ValidEnum;
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
 * <p>An audience represents a group of users defined by rules or manual CSV uploads. This request
 * provides all the necessary metadata and configuration for creating an audience.
 *
 * <p>The {@code type} field determines the audience behavior:
 *
 * <ul>
 *   <li><b>CONDITIONAL</b>: Supports batch and realtime rules (Spark/Flink processing)
 *   <li><b>STATIC</b>: Supports CSV uploads only, no rules allowed
 * </ul>
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

  /** The type of audience. Must be either CONDITIONAL or STATIC. */
  @NotNull(message = "Audience type is required")
  @ValidEnum(enumClass = AudienceType.class, message = "Type must be CONDITIONAL or STATIC")
  private String type;

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
