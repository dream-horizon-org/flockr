package com.ascend.flockr.io.request;

import io.vertx.core.json.JsonObject;
import lombok.Data;

@Data
public class OnboardDataSinkRequest {
  private String name;
  private Long typeId; // references data_connector_types.id
  private JsonObject config; // arbitrary connector specific config
}
