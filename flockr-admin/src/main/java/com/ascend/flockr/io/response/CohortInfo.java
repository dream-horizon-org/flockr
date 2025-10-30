package com.ascend.flockr.io.response;

import com.ascend.flockr.io.request.DynamicExpireConfig;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CohortInfo {

  private Long cohortId;
  private String cohortName;
  private String description;
  private Boolean expired;
  private Long expiryDate;
  private Long createdAt;
  private String createdBy;
  private Long updatedAt;
  private Long lastBatchExecutionTime;
  private Long userCount;
  private DynamicExpireConfig dynamicExpireConfig;
  private String cohortType;
  private List<String> coOwner;
  private Boolean verified;
}
