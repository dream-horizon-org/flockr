package com.ascend.flockr.io.request;

import io.vertx.core.json.JsonObject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class OnboardConnectorTypeRequest {
  @NotBlank(message = "Kind is required (SOURCE or SINK)")
  private String kind; // SOURCE or SINK

  @NotBlank(message = "Type is required")
  private String type; // e.g., KAFKA, ATHENA, S3

  @NotBlank(message = "Display name is required")
  private String displayName;

  @NotEmpty private JsonObject configSchema; // Optional JSON schema for validation
}
