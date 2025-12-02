package com.ascend.flockr.users.service.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.ascend.flockr.users.client.Aerospike;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link HealthCheckServiceImpl}.
 *
 * <p>Tests verify the health check functionality including Aerospike connection status checks.
 *
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class HealthCheckServiceImplTest {

  @Mock private Aerospike aerospikeClient;

  private HealthCheckServiceImpl service;

  @Before
  public void setUp() {
    service = new HealthCheckServiceImpl(aerospikeClient);
  }

  @Test
  public void healthCheck_WhenAerospikeIsConnected_ReturnsUpStatus() {
    // Arrange
    when(aerospikeClient.isConnected()).thenReturn(Single.just(true));

    // Act
    JsonObject result = service.healthCheck().blockingGet();

    // Assert
    assertNotNull(result);
    JsonObject status = result.getJsonObject("STATUS");
    assertNotNull(status);
    assertEquals("UP", status.getString("AEROSPIKE"));
    verify(aerospikeClient, times(1)).isConnected();
  }

  @Test
  public void healthCheck_WhenAerospikeIsNotConnected_ReturnsDownStatus() {
    // Arrange
    when(aerospikeClient.isConnected()).thenReturn(Single.just(false));

    // Act
    JsonObject result = service.healthCheck().blockingGet();

    // Assert
    assertNotNull(result);
    JsonObject status = result.getJsonObject("STATUS");
    assertNotNull(status);
    assertEquals("DOWN", status.getString("AEROSPIKE"));
    verify(aerospikeClient, times(1)).isConnected();
  }

  @Test
  public void healthCheck_WhenAerospikeCheckFails_PropagatesError() {
    // Arrange
    RuntimeException error = new RuntimeException("Connection failed");
    when(aerospikeClient.isConnected()).thenReturn(Single.error(error));

    // Act & Assert
    try {
      service.healthCheck().blockingGet();
      fail("Expected RuntimeException to be thrown");
    } catch (RuntimeException e) {
      assertEquals("Connection failed", e.getMessage());
      verify(aerospikeClient, times(1)).isConnected();
    }
  }
}
