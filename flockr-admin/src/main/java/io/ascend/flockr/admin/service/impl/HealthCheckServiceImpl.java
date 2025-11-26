package io.ascend.flockr.admin.service.impl;

import com.dream11.rest.util.ExceptionUtil;
import com.google.inject.Inject;
import io.ascend.flockr.admin.exception.ErrorEnum;
import io.ascend.flockr.admin.io.response.HealthCheckResponse;
import io.ascend.flockr.admin.repository.HealthCheckDAO;
import io.ascend.flockr.admin.service.HealthCheckService;
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
