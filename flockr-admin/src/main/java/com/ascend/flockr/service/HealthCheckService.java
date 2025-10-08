package com.ascend.flockr.service;

import com.ascend.flockr.dto.response.HealthCheckResponse;
import io.reactivex.rxjava3.core.Single;

public interface HealthCheckService {
  Single<HealthCheckResponse> healthCheck();
}
