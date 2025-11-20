package com.ascend.flockr.repository;

import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.domain.cohort.CohortOwner;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface CohortRepository {

  Single<AudienceMeta> findById(Long cohortId);

  Single<List<CohortOwner>> findOwners(Long cohortId);

  Single<Boolean> addOwner(Long cohortId, String ownerEmail, String userEmail);

  Single<Boolean> removeOwner(Long cohortId, String ownerEmail, String userEmail);
}
