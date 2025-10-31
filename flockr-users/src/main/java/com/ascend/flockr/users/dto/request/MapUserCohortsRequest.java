package com.ascend.flockr.users.dto.request;

import com.ascend.flockr.common.annotation.DateTimeFormat;
import com.ascend.flockr.common.annotation.validators.Validator;
import com.ascend.flockr.common.utils.CommonUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapUserCohortsRequest {
  @Positive private Long userId;

  private String guestId;

  @NotBlank private String cohortKey;

  @NotBlank
  //    @AcceptedValues(values = {Constants.SOURCE_DREAM11, Constants.SOURCE_FANCODE})
  //    private String source = Constants.SOURCE_DREAM11;
  private String source;

  @NotBlank
  //    @AcceptedValues(values = {Constants.ACTION_APPEND, Constants.ACTION_REMOVE})
  private String action;

  @NotBlank @DateTimeFormat private String expireAt;

  public void validate() {
    Validator.validateConstraint(this);
  }

  public Long expiryEpochFromExpireAt() {
    return CommonUtils.getEpochFromExpireAt(expireAt, action);
  }
}
