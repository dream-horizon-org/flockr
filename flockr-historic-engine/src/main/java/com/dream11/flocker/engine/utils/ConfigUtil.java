package com.dream11.flocker.engine.utils;

import com.dream11.flocker.engine.constants.Constants;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class ConfigUtil {

    private static String getAppEnvironment() {
        return System.getProperty(Constants.KEY_APP_ENV, Constants.DEFAULT_APP_ENV);
    }

    public static Config getConfigFromConfigFile(@NonNull String configFilePathFormat) {
        ConfigFactory.invalidateCaches();
        String envFile = String.format(configFilePathFormat, getAppEnvironment());
        String defaultFile = String.format(configFilePathFormat, Constants.DEFAULT_APP_ENV);

        Config envConfig = ConfigFactory.parseResources(envFile, ConfigParseOptions.defaults().setAllowMissing(true));
        Config defaultConfig = ConfigFactory.parseResources(defaultFile, ConfigParseOptions.defaults().setAllowMissing(true));

        Config config = envConfig
            .withFallback(defaultConfig)
            .withFallback(ConfigFactory.systemProperties())
            .resolve();
        log.debug("Loading config from file {} : {}", configFilePathFormat, config);
        return config;
    }

    public static <T> T getTypedConfigFromConfigFile(
        @NonNull String configFilePathFormat, Class<T> clazz) {
        Config config = getConfigFromConfigFile(configFilePathFormat);
        try {
            java.lang.reflect.Method fromConfigMethod = clazz.getMethod("fromConfig", Config.class);
            @SuppressWarnings("unchecked")
            T typedConfig = (T) fromConfigMethod.invoke(null, config);
            log.debug("Loaded Config: {}", typedConfig);
            return typedConfig;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create config object of type " + clazz.getName() +
                ". Ensure the class has a static fromConfig(Config) method.", e);
        }
    }
}

