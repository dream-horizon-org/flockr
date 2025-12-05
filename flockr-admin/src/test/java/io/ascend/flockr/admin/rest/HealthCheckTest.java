package io.ascend.flockr.admin.rest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.ascend.flockr.admin.io.ResponseEntity;
import io.ascend.flockr.admin.io.response.HealthCheckResponse;
import io.ascend.flockr.admin.service.HealthCheckService;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

/**
 * Unit tests for {@link HealthCheck}.
 *
 * <p>These tests verify that the health check endpoint correctly delegates to the service layer and
 * handles responses appropriately.
 */
public class HealthCheckTest {

  // ========================================
  // healthCheckHandle Tests
  // ========================================

  @Test
  public void healthCheckHandle_success_returnsHealthyResponse() {
    // Arrange
    HealthCheckService service = mock(HealthCheckService.class);
    HealthCheck controller = new HealthCheck(service);

    HealthCheckResponse expectedResponse = new HealthCheckResponse(true, false);
    when(service.healthCheck()).thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<HealthCheckResponse>> result =
        controller.healthCheckHandle();
    ResponseEntity.Success<HealthCheckResponse> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertTrue(response.data().isPostgresReaderUp());
    assertFalse(response.data().isUnderMaintenance());
    verify(service).healthCheck();
  }

  @Test
  public void healthCheckHandle_underMaintenance_returnsMaintenanceResponse() {
    // Arrange
    HealthCheckService service = mock(HealthCheckService.class);
    HealthCheck controller = new HealthCheck(service);

    HealthCheckResponse expectedResponse = new HealthCheckResponse(true, true);
    when(service.healthCheck()).thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<HealthCheckResponse>> result =
        controller.healthCheckHandle();
    ResponseEntity.Success<HealthCheckResponse> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertTrue(response.data().isPostgresReaderUp());
    assertTrue(response.data().isUnderMaintenance());
  }

  @Test
  public void healthCheckHandle_postgresDown_propagatesException() {
    // Arrange
    HealthCheckService service = mock(HealthCheckService.class);
    HealthCheck controller = new HealthCheck(service);

    RuntimeException expectedException = new RuntimeException("Health check failed");
    when(service.healthCheck()).thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<HealthCheckResponse>> result =
        controller.healthCheckHandle();

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Health check failed"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  @Test
  public void healthCheckHandle_serviceThrowsException_propagatesException() {
    // Arrange
    HealthCheckService service = mock(HealthCheckService.class);
    HealthCheck controller = new HealthCheck(service);

    RuntimeException expectedException = new RuntimeException("Database connection error");
    when(service.healthCheck()).thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<HealthCheckResponse>> result =
        controller.healthCheckHandle();

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Database connection error"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }
}
