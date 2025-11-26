package io.ascend.flockr.admin.service.impl;

import com.ascend.flockr.exception.ErrorEnum;
import com.ascend.flockr.io.response.HealthCheckResponse;
import com.ascend.flockr.repository.HealthCheckDAO;
import com.ascend.flockr.service.HealthCheckService;
import com.dream11.rest.util.ExceptionUtil;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of {@link HealthCheckService} providing health check operations.
 *
 * <p>This service verifies the health status of critical system dependencies including database
 * connections and checks for maintenance mode status.
 *
 * @author Flockr Team
 * @since 1.0
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class HealthCheckServiceImpl implements HealthCheckService {

  private final HealthCheckDAO healthCheckDAO;

  @Override
  public Single<HealthCheckResponse> healthCheck() {
    return Single.zip(
        healthCheckDAO.isPostgresReaderUp(),
        healthCheckDAO.isUnderMaintenance(),
        (postgresReaderUp, isUnderMaintenance) -> {
          if (!postgresReaderUp)
            throw ExceptionUtil.getException(ErrorEnum.REST_HEALTH_CHECK_FAILED);
          else return new HealthCheckResponse(true, isUnderMaintenance);
        });
  }
}
