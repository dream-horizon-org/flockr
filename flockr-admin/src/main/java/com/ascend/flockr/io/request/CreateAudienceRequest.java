package com.ascend.flockr.io.request;

import io.vertx.core.json.JsonObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAudienceRequest {
  @NotEmpty private String name;
  @NotEmpty private String description;
  private JsonObject customAudienceConfig;
  @NotEmpty private String type;

  @Max(2177452799000L)
  @NotNull
  private Long expiryDate;

  @Valid @NotEmpty private List<Long> sinkIds;
}
