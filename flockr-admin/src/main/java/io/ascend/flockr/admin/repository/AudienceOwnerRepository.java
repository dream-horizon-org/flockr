package io.ascend.flockr.admin.repository;

import io.ascend.flockr.admin.domain.audience.AudienceOwner;
import io.ascend.flockr.admin.domain.audit.AuditLogAction;
import io.ascend.flockr.admin.domain.audit.AuditLogDefinition;
import io.ascend.flockr.admin.domain.audit.AuditLogValue;
import io.ascend.flockr.admin.domain.audit.TaskType;
import io.ascend.flockr.admin.domain.rule.RuleAction;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

// AudienceOwnerRepository.java
public interface AudienceOwnerRepository {

  Single<List<AudienceOwner>> findOwners(String tenantId, String projectId, Long audienceId);

  Single<Boolean> addOwner(
      String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail);

  Single<Boolean> removeOwner(
      String tenantId, String projectId, Long audienceId, String ownerEmail, String userEmail);

  Single<List<AuditLogDefinition>> findByAudienceId( Long cohortId, Integer pageSize, Integer pageNum);

  Single<Integer> findCountByAudienceId(Long cohortId);

  Single<Long> insertAuditLog(
      Long audienceId,
      Long taskId,
      AuditLogAction action,
      AuditLogValue value,
      String createdBy,
      String name,
      RuleAction ruleAction,
      TaskType type);
}
