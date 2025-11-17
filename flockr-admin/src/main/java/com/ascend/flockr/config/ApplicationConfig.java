package com.ascend.flockr.config;

import com.ascend.flockr.config.provider.ConfigProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration class for application-level settings.
 *
 * <p>This configuration is loaded from {@code config/application/default.conf} and contains
 * general application configuration settings.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
public class ApplicationConfig {

  /**
   * Creates a provider for ApplicationConfig that loads configuration from the application config
   * directory.
   *
   * @return a ConfigProvider instance for ApplicationConfig
   */
  public static ConfigProvider<ApplicationConfig> provider() {
    return new ConfigProvider<>("application", ApplicationConfig.class);
  }
}
