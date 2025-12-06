package io.ascend.flockr.users.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.ascend.flockr.users.dto.ResponseEntity;
import io.ascend.flockr.users.service.HealthCheckService;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.CompletionStage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link HealthCheck} controller.
 *
 * <p>Tests cover health check functionality, error handling, and response formatting.
 *
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class HealthCheckTest {

  @Mock private HealthCheckService healthCheckService;

  @InjectMocks private HealthCheck controller;

  @Before
  public void setUp() {
    // Setup is handled by MockitoJUnitRunner
  }

  @Test
  public void handle_WithAerospikeUp_ReturnsSuccessResponse() throws Exception {
    // Arrange
    JsonObject healthStatus = new JsonObject();
    JsonObject status = new JsonObject();
    status.put("AEROSPIKE", "UP");
    healthStatus.put("STATUS", status);

    when(healthCheckService.healthCheck()).thenReturn(Single.just(healthStatus));

    // Act
    CompletionStage<ResponseEntity.Success<JsonObject>> responseStage = controller.handle();
    ResponseEntity.Success<JsonObject> response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    JsonObject resultStatus = response.data().getJsonObject("STATUS");
    assertNotNull(resultStatus);
    assertEquals("UP", resultStatus.getString("AEROSPIKE"));
    verify(healthCheckService, times(1)).healthCheck();
  }

  @Test
  public void handle_WithAerospikeDown_ReturnsDownStatus() throws Exception {
    // Arrange
    JsonObject healthStatus = new JsonObject();
    JsonObject status = new JsonObject();
    status.put("AEROSPIKE", "DOWN");
    healthStatus.put("STATUS", status);

    when(healthCheckService.healthCheck()).thenReturn(Single.just(healthStatus));

    // Act
    CompletionStage<ResponseEntity.Success<JsonObject>> responseStage = controller.handle();
    ResponseEntity.Success<JsonObject> response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    JsonObject resultStatus = response.data().getJsonObject("STATUS");
    assertNotNull(resultStatus);
    assertEquals("DOWN", resultStatus.getString("AEROSPIKE"));
    verify(healthCheckService, times(1)).healthCheck();
  }

  @Test
  public void handle_WithServiceError_PropagatesError() {
    // Arrange
    RuntimeException serviceError = new RuntimeException("Health check failed");
    when(healthCheckService.healthCheck()).thenReturn(Single.error(serviceError));

    // Act & Assert
    try {
      controller.handle().toCompletableFuture().get();
      fail("Expected exception to be propagated");
    } catch (Exception e) {
      assertNotNull(e);
      // Error should be mapped to RestException by ErrorHandler
    }
  }

  @Test
  public void handle_WithAerospikeConnectionError_ReturnsError() {
    // Arrange
    RuntimeException connectionError = new RuntimeException("Aerospike connection error");
    when(healthCheckService.healthCheck()).thenReturn(Single.error(connectionError));

    // Act & Assert
    try {
      controller.handle().toCompletableFuture().get();
      fail("Expected exception to be propagated");
    } catch (Exception e) {
      assertNotNull(e);
      verify(healthCheckService, times(1)).healthCheck();
    }
  }

  @Test
  public void handle_WithValidHealthStatus_ReturnsCorrectJsonStructure() throws Exception {
    // Arrange
    JsonObject healthStatus = new JsonObject();
    JsonObject status = new JsonObject();
    status.put("AEROSPIKE", "UP");
    healthStatus.put("STATUS", status);

    when(healthCheckService.healthCheck()).thenReturn(Single.just(healthStatus));

    // Act
    CompletionStage<ResponseEntity.Success<JsonObject>> responseStage = controller.handle();
    ResponseEntity.Success<JsonObject> response = responseStage.toCompletableFuture().get();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertTrue(response.data().containsKey("STATUS"));
    JsonObject resultStatus = response.data().getJsonObject("STATUS");
    assertNotNull(resultStatus);
    assertTrue(resultStatus.containsKey("AEROSPIKE"));
  }
}
