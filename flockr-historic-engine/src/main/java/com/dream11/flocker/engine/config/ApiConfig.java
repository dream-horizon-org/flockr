package com.dream11.flocker.engine.config;

import com.dream11.flocker.engine.utils.ConfigUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for REST API sink.
 * 
 * <p>This class contains all configuration parameters needed to send data to
 * a REST API endpoint, including URL, rate limiting, batching, and HTTP settings.
 * 
 * <p><b>Configuration Loading:</b>
 * <p>This class loads default values from {@code config/sink/api/default.conf}
 * and merges them with provided configuration, with provided values taking precedence.
 * 
 * <p><b>Default Values:</b>
 * <ul>
 *   <li>rateLimitPerSecond: 10 (if config file not found, otherwise from config)</li>
 *   <li>batchSize: 100 (if config file not found, otherwise from config)</li>
 *   <li>timeoutSeconds: 30 (if config file not found, otherwise from config)</li>
 *   <li>contentType: "application/json"</li>
 * </ul>
 * 
 * <p><b>Example Configuration:</b>
 * <pre>{@code
 * {
 *   "url": "http://localhost:8080/flockr/users/map-cohorts",
 *   "rateLimitPerSecond": 100,
 *   "batchSize": 100,
 *   "timeoutSeconds": 30,
 *   "contentType": "application/json",
 *   "projectKey": "tenant1_100"
 * }
 * }</pre>
 * 
 * @see com.dream11.flocker.engine.modules.sink.impl.ApiSinkImpl
 * @see com.dream11.flocker.engine.utils.ApiClient
 * @author Shivam-Raghuwanshi
 */
@Slf4j
@Data
@NoArgsConstructor
public class ApiConfig {

    private String url;
    private int rateLimitPerSecond;
    private int batchSize;
    private int timeoutSeconds;
    private String contentType;
    /** Project key for API authentication (sent as x-project-key header). */
    private String projectKey;

    /**
     * Creates an ApiConfig instance from a Typesafe Config object.
     * 
     * <p>This method:
     * <ol>
     *   <li>Loads default configuration from config file</li>
     *   <li>Merges provided config with defaults (provided values take precedence)</li>
     *   <li>Creates ApiConfig instance with merged values</li>
     * </ol>
     * 
     * <p>If the default config file cannot be loaded, hardcoded defaults are used.
     * 
     * @param config The Typesafe Config object containing API configuration.
     * @return A new ApiConfig instance with merged configuration values.
     */
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

