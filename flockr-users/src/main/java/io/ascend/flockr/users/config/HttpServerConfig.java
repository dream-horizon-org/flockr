package io.ascend.flockr.users.config;

import io.ascend.flockr.users.util.ConfigProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration class for HTTP server settings.
 *
 * <p>Contains all HTTP server configuration properties including host, port, compression settings,
 * and TCP options.
 *
 * @author Sudhanshu Rai
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

  public static ConfigProvider<HttpServerConfig> provider() {
    return new ConfigProvider<>("http-server", HttpServerConfig.class);
  }
}
