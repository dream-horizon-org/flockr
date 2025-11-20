package com.ascend.flockr.config;

import com.ascend.flockr.config.provider.ConfigProvider;
import com.typesafe.config.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SparkConfig {
    private static final String DEFAULT_HOST = "localhost";
    private static final Integer DEFAULT_PORT = 8088;
    private static final Integer DEFAULT_CONNECTION_TIMEOUT = 30000;
    private static final Integer DEFAULT_REQUEST_TIMEOUT = 60000;
    private static final Integer DEFAULT_MAX_RETRIES = 3;

    @Optional
    private String host = DEFAULT_HOST;
    @Optional private int port = DEFAULT_PORT;
    @Optional private int connectTimeout = DEFAULT_CONNECTION_TIMEOUT;
    @Optional private int requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    @Optional private int maxRetries = DEFAULT_MAX_RETRIES;

    public static ConfigProvider<SparkConfig> provider() {
        return new ConfigProvider<>("spark", SparkConfig.class);
    }

    // Helper method to get base URL
    public String getBaseUrl() {
        return String.format("http://%s:%d", host, port);
    }
}