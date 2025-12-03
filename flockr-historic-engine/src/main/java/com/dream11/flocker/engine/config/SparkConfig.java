package com.dream11.flocker.engine.config;

import com.dream11.flocker.engine.config.provider.ConfigProvider;
import com.typesafe.config.Config;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@NoArgsConstructor
public class SparkConfig {

    private String s3aMultipartPurgeAge;
    private String s3aConnectionTimeout;
    private String s3aConnectionEstablishTimeout;
    private String s3aConnectionRequestTimeout;
    private String s3aThreadsKeepaliveTime;

    private String sqlShufflePartitions;
    private Boolean sqlAdaptiveEnabled;
    private Boolean sqlAdaptiveCoalescePartitionsEnabled;

    private String networkTimeout;

    private String appName;

    public static ConfigProvider<SparkConfig> provider() {
        return new ConfigProvider<>("spark", SparkConfig.class) {
            @Override
            protected String getConfigPath() {
                return String.format("config/spark/%s.conf",
                    com.dream11.flocker.engine.constants.Constants.DEFAULT_APP_ENV);
            }
        };
    }

    public static SparkConfig fromConfig(Config config) {
        SparkConfig sparkConfig = new SparkConfig();

        sparkConfig.setS3aMultipartPurgeAge(getStringOrNull(config, "s3a.multipart.purge.age"));
        sparkConfig.setS3aConnectionTimeout(getStringOrNull(config, "s3a.connection.timeout"));
        sparkConfig.setS3aConnectionEstablishTimeout(getStringOrNull(config, "s3a.connection.establish.timeout"));
        sparkConfig.setS3aConnectionRequestTimeout(getStringOrNull(config, "s3a.connection.request.timeout"));
        sparkConfig.setS3aThreadsKeepaliveTime(getStringOrNull(config, "s3a.threads.keepalivetime"));

        sparkConfig.setSqlShufflePartitions(getStringOrNull(config, "sql.shuffle.partitions"));
        sparkConfig.setSqlAdaptiveEnabled(getBooleanOrNull(config, "sql.adaptive.enabled"));
        sparkConfig.setSqlAdaptiveCoalescePartitionsEnabled(getBooleanOrNull(config, "sql.adaptive.coalescePartitions.enabled"));

        sparkConfig.setNetworkTimeout(getStringOrNull(config, "network.timeout"));

        sparkConfig.setAppName(getStringOrNull(config, "app.name"));

        return sparkConfig;
    }

    public void applyToSessionBuilder(org.apache.spark.sql.SparkSession.Builder builder) {
        if (appName != null && !appName.trim().isEmpty()) {
            builder.appName(appName);
        }

        if (s3aMultipartPurgeAge != null) {
            builder.config("spark.hadoop.fs.s3a.multipart.purge.age", s3aMultipartPurgeAge);
        }
        if (s3aConnectionTimeout != null) {
            builder.config("spark.hadoop.fs.s3a.connection.timeout", s3aConnectionTimeout);
        }
        if (s3aConnectionEstablishTimeout != null) {
            builder.config("spark.hadoop.fs.s3a.connection.establish.timeout", s3aConnectionEstablishTimeout);
        }
        if (s3aConnectionRequestTimeout != null) {
            builder.config("spark.hadoop.fs.s3a.connection.request.timeout", s3aConnectionRequestTimeout);
        }
        if (s3aThreadsKeepaliveTime != null) {
            builder.config("spark.hadoop.fs.s3a.threads.keepalivetime", s3aThreadsKeepaliveTime);
        }

        if (sqlShufflePartitions != null) {
            builder.config("spark.sql.shuffle.partitions", sqlShufflePartitions);
        }
        if (sqlAdaptiveEnabled != null) {
            builder.config("spark.sql.adaptive.enabled", String.valueOf(sqlAdaptiveEnabled));
        }
        if (sqlAdaptiveCoalescePartitionsEnabled != null) {
            builder.config("spark.sql.adaptive.coalescePartitions.enabled", String.valueOf(sqlAdaptiveCoalescePartitionsEnabled));
        }

        if (networkTimeout != null) {
            builder.config("spark.network.timeout", networkTimeout);
        }
    }

    private static String getStringOrNull(Config config, String path) {
        return config.hasPath(path) ? config.getString(path) : null;
    }

    private static Boolean getBooleanOrNull(Config config, String path) {
        return config.hasPath(path) ? config.getBoolean(path) : null;
    }
}

