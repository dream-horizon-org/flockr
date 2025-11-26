package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.repository.HealthCheckDAO;
import com.ascend.flockr.util.MaintenanceUtil;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link HealthCheckDAO} providing health check data access operations.
 *
 * <p>This DAO checks the health status of the PostgreSQL reader connection and retrieves
 * maintenance mode status from the application context.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class HealthCheckDAOImpl implements HealthCheckDAO {

  private final PostgresReaderClient postgresReaderClient;

  @Override
  public Single<Boolean> isPostgresReaderUp() {
    return postgresReaderClient
        .isConnected()
        .onErrorReturn(
            err -> {
              log.warn("Error in connecting to Postgres-Reader: {}", err.getMessage());
              return false;
            });
  }

  @Override
  public Single<Boolean> isUnderMaintenance() {
    return Single.just(MaintenanceUtil.isUnderMaintenance(Vertx.currentContext().owner()).get());
  }
}
