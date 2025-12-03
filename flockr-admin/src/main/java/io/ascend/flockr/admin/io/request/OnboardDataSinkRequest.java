package io.ascend.flockr.admin.io.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.ascend.flockr.admin.validation.NotEmptyJsonObject;
import io.vertx.core.json.JsonObject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request object for onboarding a new data sink.
 *
 * <p>Data sinks are external destinations where computed audience data is exported.
 *
 * @since 1.0
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OnboardDataSinkRequest {
  /** The name of the data sink. Must not be empty. */
  @NotEmpty private String name;

  /** The ID of the connector type (references data_connector_types.id). */
  @NotNull private Long typeId;

  /** The connector-specific configuration (validated against the type's schema). */
  @NotEmptyJsonObject private JsonObject config;
}
