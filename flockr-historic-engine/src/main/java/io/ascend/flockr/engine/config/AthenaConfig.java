package io.ascend.flockr.engine.config;

import com.typesafe.config.Config;
import io.ascend.flockr.engine.config.provider.ConfigProvider;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for AWS Athena data source.
 *
 * <p>This class contains all configuration parameters needed to connect to and query AWS Athena,
 * including AWS credentials, region, workgroup, and output location settings.
 *
 * <p><b>Required Fields:</b>
 *
 * <ul>
 *   <li>accessKey: AWS access key ID
 *   <li>secretKey: AWS secret access key
 *   <li>region: AWS region (e.g., "us-east-1")
 *   <li>workgroup: Athena workgroup name
 *   <li>outputLocation: S3 location for Athena query results
 * </ul>
 *
 * <p><b>Optional Fields:</b>
 *
 * <ul>
 *   <li>database: Database name (can be extracted from SQL query if not provided)
 *   <li>sessionToken: AWS session token for temporary credentials
 *   <li>sqlQuery: SQL query to execute (set programmatically)
 * </ul>
 *
 * <p><b>Example Configuration:</b>
 *
 * <pre>{@code
 * {
 *   "region": "us-east-1",
 *   "workgroup": "primary",
 *   "outputLocation": "s3://bucket/athena-results/",
 *   "accessKey": "AKIA...",
 *   "secretKey": "...",
 *   "sessionToken": "optional-token",
 *   "database": "my_database"
 * }
 * }</pre>
 *
 * @see io.ascend.flockr.engine.modules.source.impl.AthenaSourceImpl
 * @author Shivam-Raghuwanshi
 */
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

  /** SQL query to execute on Athena (set programmatically, not from config). */
  private String sqlQuery;

  /**
   * Creates a ConfigProvider for Athena source configuration.
   *
   * <p>This provider loads configuration from the default config file location: {@code
   * config/source/athena/default.conf}
   *
   * @return A ConfigProvider instance for AthenaConfig.
   */
  public static ConfigProvider<AthenaConfig> providerForSource() {
    return ConfigProvider.forSource("athena", AthenaConfig.class);
  }

  /**
   * Creates an AthenaConfig instance from a Typesafe Config object.
   *
   * <p>This method extracts configuration values from the provided Config object, handling optional
   * fields gracefully (returns null if not present).
   *
   * @param config The Typesafe Config object containing Athena configuration.
   * @return A new AthenaConfig instance with values from the config.
   */
  public static AthenaConfig fromConfig(Config config) {
    AthenaConfig athenaConfig = new AthenaConfig();

    athenaConfig.setDatabase(getStringOrNull(config, "database"));
    athenaConfig.setAccessKey(getStringOrNull(config, "accessKey"));
    athenaConfig.setSecretKey(getStringOrNull(config, "secretKey"));
    athenaConfig.setSessionToken(getStringOrNull(config, "sessionToken"));
    athenaConfig.setRegion(getStringOrNull(config, "region"));
    athenaConfig.setWorkgroup(getStringOrNull(config, "workgroup"));
    athenaConfig.setOutputLocation(getStringOrNull(config, "queryOutputLocation"));
    return athenaConfig;
  }

  private static String getStringOrNull(Config config, String path) {
    return config.hasPath(path) ? config.getString(path) : null;
  }
}
