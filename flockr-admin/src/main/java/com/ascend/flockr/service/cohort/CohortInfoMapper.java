package com.ascend.flockr.service.cohort;

import com.ascend.flockr.dao.cohort.CohortOwnerInfo;
import com.ascend.flockr.io.response.CohortInfo;
import io.reactivex.functions.Function;
import java.util.function.BiFunction;
import lombok.Getter;

@Getter
class CohortInfoMapper
    implements BiFunction<CohortOwnerInfo, CohortInfo, CohortInfo>,
        Function<CohortOwnerInfo, CohortInfo> {

  @Override
  public CohortInfo apply(CohortOwnerInfo cohortOwnerInfo) {
    CohortInfo cohortInfo = new CohortInfo();
    apply(cohortOwnerInfo, cohortInfo);
    return cohortInfo;
  }

  @Override
  public CohortInfo apply(CohortOwnerInfo cohortOwnerInfo, CohortInfo cohortInfo) {
    cohortInfo.setCohortId(cohortOwnerInfo.getId());
    cohortInfo.setCohortName(cohortOwnerInfo.getName());
    cohortInfo.setDescription(cohortOwnerInfo.getDescription());
    cohortInfo.setExpired(cohortOwnerInfo.getExpired());
    cohortInfo.setExpiryDate(cohortOwnerInfo.getExpirationDate() * 1000);
    cohortInfo.setCreatedAt(cohortOwnerInfo.getCreatedAt() * 1000);
    cohortInfo.setCreatedBy(cohortOwnerInfo.getCreatedBy());
    cohortInfo.setUpdatedAt(cohortOwnerInfo.getUpdatedAt() * 1000);
    if (cohortInfo.getLastBatchExecutionTime() != null) {
      cohortInfo.setLastBatchExecutionTime(cohortOwnerInfo.getLastBatchExecutionTime() * 1000);
    }
    cohortInfo.setUserCount(cohortOwnerInfo.getUserCount());
    cohortInfo.setVerified(cohortOwnerInfo.getVerified());
    cohortInfo.setDynamicExpireConfig(cohortOwnerInfo.getDynamicExpireConfig());
    cohortInfo.setCohortType(cohortOwnerInfo.getCohortType());
    cohortInfo.setCoOwner(cohortOwnerInfo.getOwner());
    return cohortInfo;
  }
}
