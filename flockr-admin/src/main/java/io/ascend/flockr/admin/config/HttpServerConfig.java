package io.ascend.flockr.admin.config;

import com.ascend.flockr.config.provider.ConfigProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration class for HTTP server settings.
 *
 * <p>This configuration is loaded from {@code config/http-server/default.conf} and contains all
 * settings needed to configure the Vert.x HTTP server, including host, port, compression, timeouts,
 * and TCP options.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
public class HttpServerConfig {
  private String host;
  private Integer port;
  private Integer compressionLevel;
  private Boolean compressionSupported;
  private Integer idleTimeout;
  private Boolean logActivity;
  private Boolean reusePort;
  private Boolean reuseAddress;
  private Boolean tcpFastOpen;
  private Boolean tcpNoDelay;
  private Boolean tcpQuickAck;
  private Boolean tcpKeepAlive;
  private Boolean useAlpn;

  /**
   * Creates a provider for HttpServerConfig that loads configuration from the http-server config
   * directory.
   *
   * @return a ConfigProvider instance for HttpServerConfig
   */
  public static ConfigProvider<HttpServerConfig> provider() {
    return new ConfigProvider<>("http-server", HttpServerConfig.class);
  }
}
