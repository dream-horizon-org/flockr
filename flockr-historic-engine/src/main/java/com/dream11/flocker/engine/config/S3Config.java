package com.dream11.flocker.engine.config;

import com.dream11.flocker.engine.config.provider.ConfigProvider;
import com.dream11.flocker.engine.enums.FormatTypes;
import com.typesafe.config.Config;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
@NoArgsConstructor
public class S3Config {

    private String bucket;
    private String path;
    private FormatTypes format;
    private String accessKey;
    private String secretKey;
    private String region;
    private int partitions;
    private String compression;
    private Map<String, String> options;
    private String writeMode;

    public static ConfigProvider<S3Config> providerForSource() {
        return ConfigProvider.forSource("s3", S3Config.class);
    }

    public static ConfigProvider<S3Config> providerForSink() {
        return ConfigProvider.forSink("s3", S3Config.class);
    }

    private String sessionToken;

    public void configureHadoop(Configuration hadoopConf) {
        if (accessKey != null && secretKey != null) {
            hadoopConf.set("fs.s3a.access.key", accessKey);
            hadoopConf.set("fs.s3a.secret.key", secretKey);
            
            // Set session token if provided (for temporary credentials)
            if (sessionToken != null && !sessionToken.isEmpty()) {
                hadoopConf.set("fs.s3a.session.token", sessionToken);
                // Use TemporaryAWSCredentialsProvider for temporary credentials with session token
                hadoopConf.set("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.TemporaryAWSCredentialsProvider");
                log.debug("S3 credentials configured with session token - using TemporaryAWSCredentialsProvider");
            } else {
                hadoopConf.set("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
                log.debug("S3 credentials configured");
            }
        }
        if (region != null && !region.isEmpty()) {
            hadoopConf.set("fs.s3a.region", region);
            log.debug("S3 region configured: {}", region);
        }
    }

    public String getS3Path() {
        if (path != null && (path.startsWith("s3://") || path.startsWith("s3a://"))) {
            return path;
        }
        if (bucket == null || bucket.isEmpty()) {
            throw new IllegalArgumentException("S3 bucket cannot be null or empty when path is not a full S3 URI");
        }
        String cleanPath = path != null && path.startsWith("/") ? path.substring(1) : path;
        return "s3a://" + bucket + "/" + (cleanPath != null ? cleanPath : "");
    }

    public String getSparkFormat() {
        return format != null ? format.getFormat().toLowerCase() : "parquet";
    }

    public static S3Config fromConfig(Config config) {
        S3Config s3Config = new S3Config();

        s3Config.setBucket(getStringOrNull(config, "bucket"));
        s3Config.setPath(getStringOrNull(config, "path"));
        s3Config.setAccessKey(getStringOrNull(config, "accessKey"));
        s3Config.setSecretKey(getStringOrNull(config, "secretKey"));
        s3Config.setRegion(getStringOrNull(config, "region"));
        s3Config.setCompression(getStringOrNull(config, "compression"));
        s3Config.setWriteMode(getStringOrNull(config, "writeMode"));
        s3Config.setPartitions(config.hasPath("partitions") ? config.getInt("partitions") : 0);

        if (config.hasPath("format")) {
            try {
                s3Config.setFormat(FormatTypes.valueOf(config.getString("format").toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid format type: " + config.getString("format"), e);
            }
        }

        if (config.hasPath("options")) {
            Map<String, String> options = new HashMap<>();
            Config optionsConfig = config.getConfig("options");
            optionsConfig.root().keySet().forEach(key -> options.put(key, optionsConfig.getString(key)));
            s3Config.setOptions(options);
        }

        return s3Config;
    }

    private static String getStringOrNull(Config config, String path) {
        return config.hasPath(path) ? config.getString(path) : null;
    }
}
