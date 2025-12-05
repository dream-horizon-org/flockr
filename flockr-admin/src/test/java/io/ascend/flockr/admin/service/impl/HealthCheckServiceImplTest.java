package io.ascend.flockr.admin.service.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.dream11.rest.exception.RestException;
import io.ascend.flockr.admin.io.response.HealthCheckResponse;
import io.ascend.flockr.admin.repository.HealthCheckDAO;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.Test;

/**
 * Unit tests for {@link HealthCheckServiceImpl}.
 *
 * <p>These tests verify the health check service logic including database health status and
 * maintenance mode checks.
 */
public class HealthCheckServiceImplTest {

  private HealthCheckServiceImpl buildService(HealthCheckDAO healthCheckDAO) {
    return new HealthCheckServiceImpl(healthCheckDAO);
  }

  // ========================================
  // healthCheck Tests
  // ========================================

  @Test
  public void healthCheck_allHealthy_returnsHealthyResponse() {
    // Arrange
    HealthCheckDAO dao = mock(HealthCheckDAO.class);
    HealthCheckServiceImpl service = buildService(dao);

    when(dao.isPostgresReaderUp()).thenReturn(Single.just(true));
    when(dao.isUnderMaintenance()).thenReturn(Single.just(false));

    // Act
    TestObserver<HealthCheckResponse> to = service.healthCheck().test();

    // Assert
    to.assertComplete();
    to.assertValue(
        response -> {
          assertTrue(response.isPostgresReaderUp());
          assertFalse(response.isUnderMaintenance());
          return true;
        });
    verify(dao).isPostgresReaderUp();
    verify(dao).isUnderMaintenance();
  }

  @Test
  public void healthCheck_underMaintenance_returnsMaintenanceTrue() {
    // Arrange
    HealthCheckDAO dao = mock(HealthCheckDAO.class);
    HealthCheckServiceImpl service = buildService(dao);

    when(dao.isPostgresReaderUp()).thenReturn(Single.just(true));
    when(dao.isUnderMaintenance()).thenReturn(Single.just(true));

    // Act
    TestObserver<HealthCheckResponse> to = service.healthCheck().test();

    // Assert
    to.assertComplete();
    to.assertValue(
        response -> {
          assertTrue(response.isPostgresReaderUp());
          assertTrue(response.isUnderMaintenance());
          return true;
        });
  }

  @Test
  public void healthCheck_postgresDown_throwsException() {
    // Arrange
    HealthCheckDAO dao = mock(HealthCheckDAO.class);
    HealthCheckServiceImpl service = buildService(dao);

    when(dao.isPostgresReaderUp()).thenReturn(Single.just(false));
    when(dao.isUnderMaintenance()).thenReturn(Single.just(false));

    // Act
    TestObserver<HealthCheckResponse> to = service.healthCheck().test();

    // Assert
    to.assertError(RestException.class);
  }

  @Test
  public void healthCheck_postgresDownAndUnderMaintenance_throwsException() {
    // Arrange
    HealthCheckDAO dao = mock(HealthCheckDAO.class);
    HealthCheckServiceImpl service = buildService(dao);

    when(dao.isPostgresReaderUp()).thenReturn(Single.just(false));
    when(dao.isUnderMaintenance()).thenReturn(Single.just(true));

    // Act
    TestObserver<HealthCheckResponse> to = service.healthCheck().test();

    // Assert - should fail because postgres is down, regardless of maintenance status
    to.assertError(RestException.class);
  }

  @Test
  public void healthCheck_daoThrowsException_propagatesException() {
    // Arrange
    HealthCheckDAO dao = mock(HealthCheckDAO.class);
    HealthCheckServiceImpl service = buildService(dao);

    when(dao.isPostgresReaderUp())
        .thenReturn(Single.error(new RuntimeException("Connection timeout")));
    when(dao.isUnderMaintenance()).thenReturn(Single.just(false));

    // Act
    TestObserver<HealthCheckResponse> to = service.healthCheck().test();

    // Assert
    to.assertError(RuntimeException.class);
  }

  @Test
  public void healthCheck_maintenanceCheckThrowsException_propagatesException() {
    // Arrange
    HealthCheckDAO dao = mock(HealthCheckDAO.class);
    HealthCheckServiceImpl service = buildService(dao);

    when(dao.isPostgresReaderUp()).thenReturn(Single.just(true));
    when(dao.isUnderMaintenance())
        .thenReturn(Single.error(new RuntimeException("Failed to check maintenance status")));

    // Act
    TestObserver<HealthCheckResponse> to = service.healthCheck().test();

    // Assert
    to.assertError(RuntimeException.class);
  }
}
