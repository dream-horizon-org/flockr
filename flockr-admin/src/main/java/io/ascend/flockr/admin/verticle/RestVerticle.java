package io.ascend.flockr.admin.verticle;

import com.ascend.flockr.config.HttpServerConfig;
import com.ascend.flockr.constants.Constants;
import com.ascend.flockr.injection.GuiceInjector;
import com.dream11.rest.AbstractRestVerticle;
import com.dream11.rest.ClassInjector;
import com.dream11.rest.provider.JsonProvider;
import com.dream11.rest.provider.impl.JacksonProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.vertx.core.http.HttpServerOptions;

/**
 * REST API verticle that handles HTTP requests for the Flockr application.
 *
 * <p>This verticle:
 *
 * <ul>
 *   <li>Configures the HTTP server with settings from {@link HttpServerConfig}
 *   <li>Integrates Guice dependency injection for REST controllers
 *   <li>Provides Jackson-based JSON serialization/deserialization
 *   <li>Scans for REST controllers in the {@link Constants#PACKAGE_NAME} package
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
public class RestVerticle extends AbstractRestVerticle {

  /**
   * Constructs a new REST verticle with the provided HTTP server configuration.
   *
   * @param httpServerConfig the HTTP server configuration containing host, port, and other settings
   */
  @Inject
  public RestVerticle(HttpServerConfig httpServerConfig) {
    super(Constants.PACKAGE_NAME, getHttpServerOptions(httpServerConfig));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns a Guice-based injector that delegates to {@link GuiceInjector#getInstance(Class)}.
   */
  @Override
  protected ClassInjector getInjector() {
    return GuiceInjector::getInstance;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns a Jackson-based JSON provider configured with the Guice-injected ObjectMapper.
   */
  @Override
  protected JsonProvider getJsonProvider() {
    return new JacksonProvider(this.getInjector().getInstance(ObjectMapper.class));
  }

  /**
   * Converts the HTTP server configuration to Vert.x HttpServerOptions.
   *
   * @param httpServerConfig the configuration object containing server settings
   * @return configured HttpServerOptions for the Vert.x HTTP server
   */
  private static HttpServerOptions getHttpServerOptions(HttpServerConfig httpServerConfig) {
    return new HttpServerOptions()
        .setHost(httpServerConfig.getHost())
        .setPort(httpServerConfig.getPort())
        .setCompressionLevel(httpServerConfig.getCompressionLevel())
        .setCompressionSupported(httpServerConfig.getCompressionSupported())
        .setIdleTimeout(httpServerConfig.getIdleTimeout())
        .setLogActivity(httpServerConfig.getLogActivity())
        .setReusePort(httpServerConfig.getReusePort())
        .setReuseAddress(httpServerConfig.getReuseAddress())
        .setTcpFastOpen(httpServerConfig.getTcpFastOpen())
        .setTcpNoDelay(httpServerConfig.getTcpNoDelay())
        .setTcpQuickAck(httpServerConfig.getTcpQuickAck())
        .setTcpKeepAlive(httpServerConfig.getTcpKeepAlive())
        .setUseAlpn(httpServerConfig.getUseAlpn());
  }
}
