package com.ascend.flockr.io.request;

import com.ascend.flockr.validation.NotEmptyJsonObject;
import io.vertx.core.json.JsonObject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OnboardDataSinkRequest {
  @NotEmpty private String name;
  @NotNull private Long typeId; // references data_connector_types.id
  @NotEmptyJsonObject private JsonObject config; // arbitrary connector specific config
}
