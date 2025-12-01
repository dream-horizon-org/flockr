package com.dream11.flocker.engine.config;

import com.dream11.flocker.engine.config.provider.ConfigProvider;
import com.typesafe.config.Config;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@NoArgsConstructor
public class AthenaConfig {

    private String database;
    private String accessKey;
    private String secretKey;
    private String sessionToken;
    private String region;
    private String workgroup;
    private String outputLocation;
    private String sqlQuery;

    public static ConfigProvider<AthenaConfig> providerForSource() {
        return ConfigProvider.forSource("athena", AthenaConfig.class);
    }

    public static AthenaConfig fromConfig(Config config) {
        AthenaConfig athenaConfig = new AthenaConfig();

        athenaConfig.setDatabase(getStringOrNull(config, "database"));
        athenaConfig.setAccessKey(getStringOrNull(config, "accessKey"));
        athenaConfig.setSecretKey(getStringOrNull(config, "secretKey"));
        athenaConfig.setSessionToken(getStringOrNull(config, "sessionToken"));
        athenaConfig.setRegion(getStringOrNull(config, "region"));
        athenaConfig.setWorkgroup(getStringOrNull(config, "workgroup"));
        athenaConfig.setOutputLocation(getStringOrNull(config, "outputLocation"));
        return athenaConfig;
    }

    private static String getStringOrNull(Config config, String path) {
        return config.hasPath(path) ? config.getString(path) : null;
    }
}
