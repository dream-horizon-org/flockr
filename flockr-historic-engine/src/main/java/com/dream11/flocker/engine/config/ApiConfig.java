package com.dream11.flocker.engine.config;

import com.dream11.flocker.engine.utils.ConfigUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@NoArgsConstructor
public class ApiConfig {

    private String url;
    private int rateLimitPerSecond;
    private int batchSize;
    private int timeoutSeconds;
    private String contentType;
    private String projectKey;

    public static ApiConfig fromConfig(Config config) {
        ApiConfig apiConfig = new ApiConfig();

        Config defaultConfig = loadDefaultConfig();

        Config mergedConfig = config.withFallback(defaultConfig);

        apiConfig.setUrl(getStringOrNull(mergedConfig, "url"));
        apiConfig.setRateLimitPerSecond(mergedConfig.getInt("rateLimitPerSecond"));
        apiConfig.setBatchSize(mergedConfig.getInt("batchSize"));
        apiConfig.setTimeoutSeconds(mergedConfig.getInt("timeoutSeconds"));
        apiConfig.setContentType(getStringOrNull(mergedConfig, "contentType") != null
            ? getStringOrNull(mergedConfig, "contentType") : "application/json");
        apiConfig.setProjectKey(getStringOrNull(mergedConfig, "projectKey"));

        return apiConfig;
    }

    private static Config loadDefaultConfig() {
        try {
            return ConfigUtil.getConfigFromConfigFile("config/sink/api/%s.conf");
        } catch (Exception e) {
            log.warn("Failed to load default API config, using hardcoded defaults: {}", e.getMessage());
            return ConfigFactory.parseString(
                "rateLimitPerSecond = 10\n" +
                "batchSize = 100\n" +
                "timeoutSeconds = 30\n" +
                "contentType = \"application/json\""
            );
        }
    }

    private static String getStringOrNull(Config config, String path) {
        return config.hasPath(path) ? config.getString(path) : null;
    }
}

