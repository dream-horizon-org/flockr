package com.dream11.flocker.engine.config;

import com.dream11.flocker.engine.config.provider.ConfigProvider;
import com.dream11.flocker.engine.enums.FormatTypes;
import com.typesafe.config.Config;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;

/**
 * Configuration for S3 data source or sink.
 *
 * <p>This class contains all configuration parameters needed to read from or write to Amazon S3,
 * including bucket, path, format, compression, and AWS credentials.
 *
 * <p><b>Usage:</b>
 *
 * <p>This configuration can be used for both sources and sinks:
 *
 * <ul>
 *   <li><b>Source</b>: Read data from S3 (Parquet, CSV, etc.)
 *   <li><b>Sink</b>: Write data to S3 (Parquet, CSV, etc.)
 * </ul>
 *
 * <p><b>Required Fields (for sink):</b>
 *
 * <ul>
 *   <li>bucket: S3 bucket name
 *   <li>path: S3 path within the bucket
 * </ul>
 *
 * <p><b>Optional Fields:</b>
 *
 * <ul>
 *   <li>format: File format (PARQUET, CSV, JSON) - defaults to PARQUET
 *   <li>compression: Compression type (snappy, gzip, etc.)
 *   <li>accessKey: AWS access key ID (inherited from source if not provided)
 *   <li>secretKey: AWS secret access key (inherited from source if not provided)
 *   <li>sessionToken: AWS session token for temporary credentials
 *   <li>region: AWS region
 *   <li>partitions: Number of partitions for writing (0 = auto)
 *   <li>writeMode: Write mode (append, overwrite, etc.) - defaults to append
 *   <li>options: Additional Spark options as key-value pairs
 * </ul>
 *
 * <p><b>Example Configuration:</b>
 *
 * <pre>{@code
 * {
 *   "bucket": "my-bucket",
 *   "path": "output/path",
 *   "format": "parquet",
 *   "compression": "snappy",
 *   "writeMode": "append"
 * }
 * }</pre>
 *
 * @see com.dream11.flocker.engine.modules.source.impl.S3SourceImpl
 * @see com.dream11.flocker.engine.modules.sink.impl.S3SinkImpl
 * @author Shivam-Raghuwanshi
 */
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

  /** Write mode for S3 sink (append, overwrite, etc.). */
  private String writeMode;

  /**
   * Creates a ConfigProvider for S3 source configuration.
   *
   * <p>This provider loads configuration from the default config file location: {@code
   * config/source/s3/default.conf}
   *
   * @return A ConfigProvider instance for S3Config.
   */
  public static ConfigProvider<S3Config> providerForSource() {
    return ConfigProvider.forSource("s3", S3Config.class);
  }

  /**
   * Creates a ConfigProvider for S3 sink configuration.
   *
   * <p>This provider loads configuration from the default config file location: {@code
   * config/sink/s3/default.conf}
   *
   * @return A ConfigProvider instance for S3Config.
   */
  public static ConfigProvider<S3Config> providerForSink() {
    return ConfigProvider.forSink("s3", S3Config.class);
  }

  /** AWS session token for temporary credentials. */
  private String sessionToken;

  /**
   * Configures Hadoop/Spark S3A filesystem settings with AWS credentials.
   *
   * <p>This method sets the following Hadoop configuration properties:
   *
   * <ul>
   *   <li>fs.s3a.access.key: AWS access key
   *   <li>fs.s3a.secret.key: AWS secret key
   *   <li>fs.s3a.session.token: AWS session token (if provided)
   *   <li>fs.s3a.aws.credentials.provider: Credentials provider class
   *   <li>fs.s3a.region: AWS region (if provided)
   * </ul>
   *
   * <p>If a session token is provided, it uses TemporaryAWSCredentialsProvider, otherwise it uses
   * SimpleAWSCredentialsProvider.
   *
   * @param hadoopConf The Hadoop Configuration object to configure.
   */
  public void configureHadoop(Configuration hadoopConf) {
    if (accessKey != null && secretKey != null) {
      hadoopConf.set("fs.s3a.access.key", accessKey);
      hadoopConf.set("fs.s3a.secret.key", secretKey);

      if (sessionToken != null && !sessionToken.isEmpty()) {
        hadoopConf.set("fs.s3a.session.token", sessionToken);
        hadoopConf.set(
            "fs.s3a.aws.credentials.provider",
            "org.apache.hadoop.fs.s3a.TemporaryAWSCredentialsProvider");
        log.debug(
            "S3 credentials configured with session token - using TemporaryAWSCredentialsProvider");
      } else {
        hadoopConf.set(
            "fs.s3a.aws.credentials.provider",
            "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
        log.debug("S3 credentials configured");
      }
    }
    if (region != null && !region.isEmpty()) {
      hadoopConf.set("fs.s3a.region", region);
      log.debug("S3 region configured: {}", region);
    }
  }

  /**
   * Gets the full S3 path (URI) for this configuration.
   *
   * <p>If the path already starts with "s3://" or "s3a://", it is returned as-is. Otherwise, it
   * constructs the path as "s3a://{bucket}/{path}".
   *
   * <p>The path is cleaned to remove leading slashes before construction.
   *
   * @return The full S3 URI path.
   * @throws IllegalArgumentException If bucket is null or empty when path is not a full URI.
   */
  public String getS3Path() {
    if (path != null && (path.startsWith("s3://") || path.startsWith("s3a://"))) {
      return path;
    }
    if (bucket == null || bucket.isEmpty()) {
      throw new IllegalArgumentException(
          "S3 bucket cannot be null or empty when path is not a full S3 URI");
    }
    String cleanPath = path != null && path.startsWith("/") ? path.substring(1) : path;
    return "s3a://" + bucket + "/" + (cleanPath != null ? cleanPath : "");
  }

  /**
   * Gets the Spark format string for this configuration.
   *
   * <p>Returns the format name in lowercase (e.g., "parquet", "csv", "json"). Defaults to "parquet"
   * if format is not specified.
   *
   * @return The Spark format string.
   */
  public String getSparkFormat() {
    return format != null ? format.getFormat().toLowerCase() : "parquet";
  }

  /**
   * Creates an S3Config instance from a Typesafe Config object.
   *
   * <p>This method extracts configuration values from the provided Config object, including
   * optional fields like format, compression, partitions, and options.
   *
   * @param config The Typesafe Config object containing S3 configuration.
   * @return A new S3Config instance with values from the config.
   * @throws IllegalArgumentException If an invalid format type is specified.
   */
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
