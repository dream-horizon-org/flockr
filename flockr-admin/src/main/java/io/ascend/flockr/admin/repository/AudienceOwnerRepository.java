package io.ascend.flockr.admin.repository;

import io.ascend.flockr.admin.io.request.AudienceOwner;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

// AudienceOwnerRepository.java
public interface AudienceOwnerRepository {

  Single<List<AudienceOwner>> findOwners(String tenantId, String projectId, Long audienceId);

  Single<Boolean> addOwner(
      String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail);

  Single<Boolean> removeOwner(
      String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail);
}
