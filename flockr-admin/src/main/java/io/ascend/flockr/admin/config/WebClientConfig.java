package io.ascend.flockr.admin.config;

import com.typesafe.config.Optional;
import io.ascend.flockr.admin.config.provider.ConfigProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration class for Vert.x Web Client settings.
 *
 * <p>This configuration is loaded from {@code config/webclient/default.conf} and contains all
 * settings needed to configure the HTTP client, including connection timeouts, pool sizes,
 * keep-alive settings, and HTTP pipelining options.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
public class WebClientConfig {
  private static final Integer DEFAULT_CONNECTION_TIMEOUT = 1000;
  private static final Integer DEFAULT_MAX_POOL_SIZE = 32;
  private static final Boolean DEFAULT_LOG_ACTIVITY = Boolean.FALSE;
  private static final Boolean DEFAULT_KEEP_ALIVE = Boolean.TRUE;
  private static final Integer DEFAULT_KEEP_ALIVE_TIMEOUT = 10;
  private static final Integer DEFAULT_MAX_WAIT_QUEUE_SIZE = 100;
  private static final Boolean DEFAULT_PIPELINING = Boolean.FALSE;
  private static final Integer DEFAULT_PIPELINING_LIMIT = 8;

  @Optional private int connectTimeout = DEFAULT_CONNECTION_TIMEOUT;
  @Optional private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
  @Optional private boolean logActivity = DEFAULT_LOG_ACTIVITY;
  @Optional private boolean keepAlive = DEFAULT_KEEP_ALIVE;
  @Optional private int keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;
  @Optional private int maxWaitQueueSize = DEFAULT_MAX_WAIT_QUEUE_SIZE;
  @Optional private boolean pipelining = DEFAULT_PIPELINING;
  @Optional private int pipeliningLimit = DEFAULT_PIPELINING_LIMIT;

  /**
   * Creates a provider for WebClientConfig that loads configuration from the webclient config
   * directory.
   *
   * @return a ConfigProvider instance for WebClientConfig
   */
  public static ConfigProvider<WebClientConfig> provider() {
    return new ConfigProvider<>("webclient", WebClientConfig.class);
  }
}
