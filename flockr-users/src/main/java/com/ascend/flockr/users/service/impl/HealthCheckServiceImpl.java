package com.ascend.flockr.users.service.impl;

import com.ascend.flockr.common.client.Aerospike;
import com.ascend.flockr.users.service.HealthCheckService;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of {@link HealthCheckService}.
 *
 * <p>This implementation checks the health status of Aerospike connection and returns a health
 * status response.
 *
 * @since 1.0
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class HealthCheckServiceImpl implements HealthCheckService {

  private final Aerospike aerospikeClient;

  /**
   * {@inheritDoc}
   *
   * <p>Checks the Aerospike connection status and returns a health status JSON object. The status
   * will be "UP" if Aerospike is connected, "DOWN" otherwise.
   */
  @Override
  public Single<JsonObject> healthCheck() {
    return aerospikeClient
        .isConnected()
        .map(
            isConnected -> {
              String statusValue = isConnected ? "UP" : "DOWN";
              return new JsonObject().put("STATUS", new JsonObject().put("AEROSPIKE", statusValue));
            });
  }
}
