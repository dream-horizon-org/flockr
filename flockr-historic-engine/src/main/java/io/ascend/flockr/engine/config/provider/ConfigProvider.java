package io.ascend.flockr.engine.config.provider;

import com.google.inject.Provider;
import io.ascend.flockr.engine.constants.Constants;
import io.ascend.flockr.engine.utils.ConfigUtil;
import lombok.NonNull;

public class ConfigProvider<T> implements Provider<T> {

  private final String configDirectory;
  private final Class<T> clazz;
  protected String configPathFormat = "config/%s/%s.conf";

  public ConfigProvider(@NonNull String configDirectory, @NonNull Class<T> clazz) {
    this.configDirectory = configDirectory;
    this.clazz = clazz;
  }

  @Override
  public T get() {
    return ConfigUtil.getTypedConfigFromConfigFile(getConfigPath(), clazz);
  }

  protected String getConfigPath() {
    return String.format(configPathFormat, configDirectory, Constants.DEFAULT_APP_ENV);
  }

  public static <T> ConfigProvider<T> forSource(
      @NonNull String sourceType, @NonNull Class<T> clazz) {
    return new ConfigProvider<>("source/" + sourceType, clazz);
  }

  public static <T> ConfigProvider<T> forSink(@NonNull String sinkType, @NonNull Class<T> clazz) {
    return new ConfigProvider<>("sink/" + sinkType, clazz);
  }
}
