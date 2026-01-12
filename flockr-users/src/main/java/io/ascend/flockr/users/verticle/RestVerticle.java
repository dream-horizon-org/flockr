package io.ascend.flockr.users.verticle;

import com.dream11.rest.AbstractRestVerticle;
import com.dream11.rest.ClassInjector;
import com.dream11.rest.provider.JsonProvider;
import com.dream11.rest.provider.impl.JacksonProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.ascend.flockr.users.config.HttpServerConfig;
import io.ascend.flockr.users.guice.GuiceInjector;
import io.vertx.core.http.HttpServerOptions;

/**
 * REST API Verticle for handling HTTP requests.
 *
 * <p>This verticle extends AbstractRestVerticle and configures the REST framework to scan and
 * handle REST endpoints. It sets up dependency injection, JSON providers, and HTTP server options.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
public class RestVerticle extends AbstractRestVerticle {
  private static final String PACKAGE_NAME = "io.ascend.flockr.users";

  @Inject
  public RestVerticle(HttpServerConfig httpServerConfig) {
    super(PACKAGE_NAME, getHttpServerOptions(httpServerConfig));
  }

  @Override
  protected ClassInjector getInjector() {
    return GuiceInjector::getInstance;
  }

  @Override
  protected JsonProvider getJsonProvider() {
    return new JacksonProvider(this.getInjector().getInstance(ObjectMapper.class));
  }

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
