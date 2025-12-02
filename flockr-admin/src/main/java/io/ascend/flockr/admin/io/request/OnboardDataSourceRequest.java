package io.ascend.flockr.admin.io.request;

import io.ascend.flockr.admin.validation.NotEmptyJsonObject;
import io.vertx.core.json.JsonObject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request object for onboarding a new data source.
 *
 * <p>Data sources are external systems from which data can be read for rule processing.
 *
 * @since 1.0
 */
@Data
public class OnboardDataSourceRequest {
  /** The name of the data source. Must not be empty. */
  @NotEmpty private String name;

  /** The ID of the connector type (references data_connector_types.id). */
  @NotNull private Long typeId;

  /** The connector-specific configuration (validated against the type's schema). */
  @NotEmptyJsonObject private JsonObject config;
}
