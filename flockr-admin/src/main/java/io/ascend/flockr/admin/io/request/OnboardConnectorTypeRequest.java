package io.ascend.flockr.admin.io.request;

import io.ascend.flockr.admin.validation.NotEmptyJsonObject;
import io.vertx.core.json.JsonObject;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request object for onboarding a new connector type.
 *
 * <p>Connector types define categories of data sources or sinks (e.g., KAFKA, POSTGRES, S3) along
 * with their JSON schema for configuration validation.
 *
 * @since 1.0
 */
@Data
public class OnboardConnectorTypeRequest {
  /** The kind of connector: "SOURCE" or "SINK". */
  @NotBlank(message = "Kind is required (SOURCE or SINK)")
  private String kind;

  /** The type identifier (e.g., "KAFKA", "ATHENA", "S3"). */
  @NotBlank(message = "Type is required")
  private String type;

  /** The human-readable display name for the connector type. */
  @NotBlank(message = "Display name is required")
  private String displayName;

  /** The JSON schema used to validate connector configurations. */
  @NotEmptyJsonObject private JsonObject configSchema;
}
