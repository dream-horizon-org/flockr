package com.ascend.flockr.users.dto.request;

import com.ascend.flockr.common.annotation.DateTimeFormat;
import com.ascend.flockr.common.annotation.validators.Validator;
import com.ascend.flockr.common.utils.CommonUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for mapping a user to a cohort.
 *
 * <p>Contains all information needed to assign or remove a user from a cohort, including user
 * identifier, tenant and project identifiers for multi-tenant isolation, cohort name, action type,
 * and expiry information.
 *
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapUserCohortsRequest {
  /** User ID (required). */
  @Positive private Long userId;

  /** Tenant ID (required for multi-tenant isolation). */
  @NotBlank private String tenantId;

  /** Project ID (required for multi-tenant isolation). */
  @Positive private Long projectId;

  /** Cohort name to assign/remove user from. */
  @NotBlank private String cohortKey;

  /** Source identifier (e.g., "Dream11", "FanCode"). */
  @NotBlank
  //    @AcceptedValues(values = {Constants.SOURCE_DREAM11, Constants.SOURCE_FANCODE})
  //    private String source = Constants.SOURCE_DREAM11;
  private String source;

  /** Action type: "append" to add user, "remove" to remove user. */
  @NotBlank
  //    @AcceptedValues(values = {Constants.ACTION_APPEND, Constants.ACTION_REMOVE})
  private String action;

  /** Expiry time in format "yyyy-MM-dd HH:mm:ss" (required for append action). */
  @NotBlank @DateTimeFormat private String expireAt;

  /**
   * Validates the request using Bean Validation constraints.
   *
   * @throws jakarta.validation.ConstraintViolationException if validation fails
   */
  public void validate() {
    Validator.validateConstraint(this);
  }

  /**
   * Converts the expireAt string to epoch milliseconds.
   *
   * @return expiry time in epoch milliseconds
   * @throws IllegalArgumentException if expireAt format is invalid
   */
  public Long expiryEpochFromExpireAt() {
    return CommonUtils.getEpochFromExpireAt(expireAt, action);
  }
}
