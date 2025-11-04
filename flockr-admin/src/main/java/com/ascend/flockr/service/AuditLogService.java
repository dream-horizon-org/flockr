package com.ascend.flockr.service;

import com.ascend.flockr.io.response.AuditLogResponse;
import com.ascend.flockr.io.response.PaginatedResponse;
import com.ascend.flockr.model.AuditLog.AuditLog;
import io.reactivex.Single;

public interface AuditLogService {

  Single<PaginatedResponse<AuditLogResponse>> findByCohortId(
      Long cohortId, Integer pageSize, Integer pageNum, Boolean withPagination);

  void create(AuditLog auditLog);
}
