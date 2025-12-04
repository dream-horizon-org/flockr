package io.ascend.flockr.users.util;

import com.google.inject.Provider;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic provider for loading typed configuration objects from config files.
 *
 * <p>This provider loads configuration from TypeSafe Config files and converts them to typed Java
 * objects. It supports environment-specific configuration files with fallback to default
 * configuration.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 * @param <T> the type of configuration object to provide
 */
@Slf4j
@Getter
public class ConfigProvider<T> implements Provider<T> {

  private final String configDirectory;
  private final Class<T> clazz;
  protected String configPathFormat = "config/%s/%%s.conf";

  public ConfigProvider(String configDirectory, Class<T> clazz) {
    this.configDirectory = configDirectory;
    this.clazz = clazz;
  }

  @Override
  public T get() {
    return getTypedConfigFromConfigFile(getConfigPath(), clazz);
  }

  protected String getConfigPath() {
    return configPathFormat.formatted(configDirectory);
  }

  private static String getAppEnvironment() {
    return System.getProperty("app.environment", "default");
  }

  public static <T> T getTypedConfigFromConfigFile(String configFilePathFormat, Class<T> clazz) {
    ConfigFactory.invalidateCaches();
    String envFile = String.format(configFilePathFormat, getAppEnvironment());
    String defaultFile = String.format(configFilePathFormat, "default");
    Config config =
        ConfigFactory.load(envFile)
            .withFallback(
                ConfigFactory.load(
                    defaultFile,
                    ConfigParseOptions.defaults().setAllowMissing(true),
                    ConfigResolveOptions.defaults().setAllowUnresolved(true)))
            .resolve();
    log.debug("Loading config from file {} : {}", configFilePathFormat, config);
    T typedConfig = ConfigBeanFactory.create(config, clazz);
    log.debug("Loaded Config: {}", typedConfig);
    return typedConfig;
  }
}
