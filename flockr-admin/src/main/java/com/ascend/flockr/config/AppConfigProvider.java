package com.ascend.flockr.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;
import java.io.File;
import lombok.SneakyThrows;

public class AppConfigProvider implements Provider<AppConfig> {

  private static AppConfig appConfig;

  @Override
  public AppConfig get() {
    if (appConfig == null) {
      setAppConfig();
    }

    return appConfig;
  }

  @SneakyThrows
  public static AppConfig getAppConfig() {
    if (appConfig == null) {
      setAppConfig();
    }

    return appConfig;
  }

  @SneakyThrows
  public static synchronized void setAppConfig() {
    if (appConfig == null) {
      ObjectMapper mapper = new ObjectMapper();
      appConfig = mapper.readValue(new File("config/application.json"), AppConfig.class);

      //            appConfig =
      //                    AppContext.getInstance(ConfigProvider.class)
      //                            .getConfig("config/application", "application",
      // AppConfig.class);
    }
  }
}
