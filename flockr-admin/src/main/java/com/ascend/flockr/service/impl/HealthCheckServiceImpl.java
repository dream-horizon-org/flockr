package com.ascend.flockr.service.impl;

import com.ascend.flockr.dto.response.HealthCheckResponse;
import com.ascend.flockr.exception.ErrorEnum;
import com.ascend.flockr.dao.HealthCheckDAO;
import com.ascend.flockr.service.HealthCheckService;
import com.dream11.rest.util.ExceptionUtil;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class HealthCheckServiceImpl implements HealthCheckService {

  private final HealthCheckDAO healthCheckDAO;

  @Override
  public Single<HealthCheckResponse> healthCheck() {
    return Single.zip(
                healthCheckDAO.isMySQLReaderConnected(),
                                healthCheckDAO.isAerospikeConnected(),
                        healthCheckDAO.isUnderMaintenance(),
        (
                        isMySQLReaderUp,
                                                isAerospikeUp,
                                    isUnderMaintenance
         ) -> {
          if (true
                            && !isMySQLReaderUp
                                                        && !isAerospikeUp
                                      )
            throw ExceptionUtil.getException(ErrorEnum.REST_HEALTH_CHECK_FAILED);
          else return new HealthCheckResponse(
                            isMySQLReaderUp,
                                                        isAerospikeUp,
                                          isUnderMaintenance
          );
        });
  }
}
