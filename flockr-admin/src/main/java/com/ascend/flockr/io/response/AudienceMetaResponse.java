package com.ascend.flockr.io.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object containing basic audience metadata including rule count. Used for listing
 * audiences with essential information.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AudienceMetaResponse {
  private Long audienceId;
  private String name;
  private String description;
  private String type;
  private Boolean verified;
  private Long userCount;
  private Long ruleCount;
  private Long expireDate;
  private Long createdAt;
  private Long updatedAt;
  private String createdBy;
}
