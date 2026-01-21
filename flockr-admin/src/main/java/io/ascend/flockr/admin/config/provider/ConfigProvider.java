package io.ascend.flockr.admin.config.provider;

import com.google.inject.Provider;
import io.ascend.flockr.admin.util.ConfigurationUtil;
import lombok.Getter;

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
    return ConfigurationUtil.getTypedConfigFromFile(getConfigPath(), clazz);
  }

  protected String getConfigPath() {
    return configPathFormat.formatted(configDirectory);
  }
}
