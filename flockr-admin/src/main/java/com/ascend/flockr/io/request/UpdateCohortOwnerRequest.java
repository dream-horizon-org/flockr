package com.ascend.flockr.io.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCohortOwnerRequest {

  @NotNull private UpdateCohortOwnerAction action;
  @NotEmpty private String email;
}
