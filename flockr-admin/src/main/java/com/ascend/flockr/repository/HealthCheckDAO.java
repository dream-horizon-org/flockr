package com.ascend.flockr.repository;

import io.reactivex.rxjava3.core.Single;

/**
 * Data Access Object (DAO) interface for health check operations.
 *
 * <p>This DAO provides methods to verify the health status of critical system dependencies and
 * maintenance states.
 *
 * @author Flockr Team
 * @since 1.0
 */
public interface HealthCheckDAO {
  /**
   * Checks if the PostgreSQL reader connection is available and functioning.
   *
   * @return a Single emitting true if the Postgres reader is up and responding, false otherwise
   */
  Single<Boolean> isPostgresReaderUp();

  /**
   * Checks if the application is currently under maintenance mode.
   *
   * @return a Single emitting true if the system is under maintenance, false otherwise
   */
  Single<Boolean> isUnderMaintenance();
}
