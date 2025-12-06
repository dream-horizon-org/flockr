package io.ascend.flockr.users.module;

import com.google.inject.AbstractModule;
import io.ascend.flockr.users.config.AerospikeConfig;
import io.ascend.flockr.users.config.HttpServerConfig;
import io.ascend.flockr.users.util.ConfigProvider;

/**
 * Guice module for configuration bindings.
 *
 * <p>Binds configuration classes that are loaded from config files.
 *
 * @author Sudhanshu Rai
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
