package io.ascend.flockr.admin.io.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAudienceOwnerRequest {

  @NotNull private UpdateAudienceOwnerAction action;
  @NotEmpty private String email;
}
