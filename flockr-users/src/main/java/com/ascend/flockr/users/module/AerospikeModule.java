package com.ascend.flockr.users.module;

import com.ascend.flockr.common.client.Aerospike;
import com.ascend.flockr.common.client.impl.AerospikeImpl;
import com.ascend.flockr.common.config.AerospikeConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.d11.aerospike.client.AerospikeClient;
import io.d11.aerospike.client.AerospikeConnectOptions;
import io.vertx.core.Vertx;

public class AerospikeModule extends AbstractModule {
  private final Vertx vertx;

  public AerospikeModule(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  protected void configure() {
    bind(Aerospike.class).to(AerospikeImpl.class).in(Singleton.class);
  }

  @Provides
  AerospikeClient provideFlockrAerospikeClient(AerospikeConnectOptions connectOptions) {
    return AerospikeClient.create(vertx, connectOptions);
  }

  @Provides
  AerospikeConnectOptions provideAerospikeConnectOptions(AerospikeConfig config) {

    AerospikeConnectOptions connectOptions =
        new AerospikeConnectOptions()
            .setHost(config.getHost())
            .setEventLoopSize(config.getEventLoopSize())
            .setMaxCommandsInProcess(config.getMaxCommandsInProcess())
            .setMaxConnsPerNode(config.getMaxConnectionsPerNode());
    return connectOptions;
  }
}
