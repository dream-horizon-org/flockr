package io.ascend.flockr.admin.config;

import com.ascend.flockr.config.provider.ConfigProvider;
import com.typesafe.config.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration class for Apache Flink client. Contains connection settings and timeouts for Flink
 * REST API.
 */
@Data
@NoArgsConstructor
public class FlinkConfig {
  private static final String DEFAULT_HOST = "localhost";
  private static final Integer DEFAULT_PORT = 8081;
  private static final Integer DEFAULT_CONNECTION_TIMEOUT = 30000;
  private static final Integer DEFAULT_REQUEST_TIMEOUT = 60000;
  private static final Integer DEFAULT_MAX_POOL_SIZE = 16;
  private static final Boolean DEFAULT_LOG_ACTIVITY = Boolean.TRUE;
  private static final Boolean DEFAULT_KEEP_ALIVE = Boolean.TRUE;
  private static final Integer DEFAULT_KEEP_ALIVE_TIMEOUT = 60;
  private static final Integer DEFAULT_SAVEPOINT_POLL_INTERVAL = 2000;
  private static final Integer DEFAULT_SAVEPOINT_MAX_RETRIES = 30;

  @Optional private String host = DEFAULT_HOST;
  @Optional private int port = DEFAULT_PORT;
  @Optional private int connectTimeout = DEFAULT_CONNECTION_TIMEOUT;
  @Optional private int requestTimeout = DEFAULT_REQUEST_TIMEOUT;
  @Optional private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
  @Optional private boolean logActivity = DEFAULT_LOG_ACTIVITY;
  @Optional private boolean keepAlive = DEFAULT_KEEP_ALIVE;
  @Optional private int keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;
  @Optional private int savepointPollInterval = DEFAULT_SAVEPOINT_POLL_INTERVAL;
  @Optional private int savepointMaxRetries = DEFAULT_SAVEPOINT_MAX_RETRIES;

  public static ConfigProvider<FlinkConfig> provider() {
    return new ConfigProvider<>("flink", FlinkConfig.class);
  }
}
