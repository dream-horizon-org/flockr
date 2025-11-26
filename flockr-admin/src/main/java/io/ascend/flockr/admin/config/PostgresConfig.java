package io.ascend.flockr.admin.config;

import com.ascend.flockr.config.provider.ConfigProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration class for PostgreSQL database connections.
 *
 * <p>This configuration is loaded from {@code config/postgres/default.conf} and contains settings
 * for both reader and writer database connections, including connection options and connection pool
 * settings.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
public class PostgresConfig {

  /** Configuration for the reader database connection pool. */
  private BaseConfig readerConfig;

  /** Configuration for the writer database connection pool. */
  private BaseConfig writerConfig;

  /**
   * Base configuration for a PostgreSQL connection pool.
   *
   * <p>Contains connection options and pool options for either reader or writer connections.
   */
  @Data
  @NoArgsConstructor
  public static class BaseConfig {
    /** Connection options for establishing database connections. */
    private ConnectOptions connectOptions;

    /** Pool options for managing the connection pool. */
    private PoolOptions poolOptions;

    /** Number of retry attempts for failed operations. */
    private Integer retryCount;
  }

  /**
   * Connection options for PostgreSQL database connections.
   *
   * <p>Contains host, port, credentials, database name, and connection-specific settings.
   */
  @Data
  @NoArgsConstructor
  public static class ConnectOptions {
    private String host;
    private Integer port;
    private String user;
    private String password;
    private String database;
    private String metricsName;
    private Integer connectTimeout;
    private Boolean useAffectedRows;
    private Boolean cachePreparedStatements;
  }

  /**
   * Connection pool options for managing database connection pools.
   *
   * <p>Controls the size and behavior of the connection pool.
   */
  @Data
  @NoArgsConstructor
  public static class PoolOptions {
    private Integer maxSize;
    private Integer maxWaitQueueSize;
  }

  /**
   * Creates a provider for PostgresConfig that loads configuration from the postgres config
   * directory.
   *
   * @return a ConfigProvider instance for PostgresConfig
   */
  public static ConfigProvider<PostgresConfig> provider() {
    return new ConfigProvider<>("postgres", PostgresConfig.class);
  }
}
