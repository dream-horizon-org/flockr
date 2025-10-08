package com.ascend.flockr.config;

import com.ascend.flockr.config.provider.ConfigProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApplicationConfig {

  public static ConfigProvider<ApplicationConfig> provider() {
    return new ConfigProvider<>("application", ApplicationConfig.class);
  }
}
