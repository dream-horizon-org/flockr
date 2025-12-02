package com.ascend.flockr.users.module;

import com.ascend.flockr.users.config.AerospikeConfig;
import com.ascend.flockr.users.config.HttpServerConfig;
import com.ascend.flockr.users.util.ConfigProvider;
import com.google.inject.AbstractModule;

/**
 * Guice module for configuration bindings.
 *
 * <p>Binds configuration classes that are loaded from config files.
 *
 * @since 1.0
 */
public class ConfigModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AerospikeConfig.class)
        .toProvider(
            () ->
                ConfigProvider.getTypedConfigFromConfigFile(
                    "config/aerospike/%s.conf", AerospikeConfig.class))
        .asEagerSingleton();

    bind(HttpServerConfig.class).toProvider(HttpServerConfig.provider()).asEagerSingleton();
  }
}
