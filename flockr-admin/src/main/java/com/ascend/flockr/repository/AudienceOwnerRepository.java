package com.ascend.flockr.repository;

import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.domain.cohort.AudienceOwner;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

// AudienceOwnerRepository.java
public interface AudienceOwnerRepository {

    Single<AudienceMeta> findById(String tenantId, String projectId, Long audienceId);

    Single<List<AudienceOwner>> findOwners(String tenantId, String projectId, Long audienceId);

    Single<Boolean> addOwner(String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail);

    Single<Boolean> removeOwner(String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail);
}
