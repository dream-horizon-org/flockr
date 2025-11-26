package com.ascend.flockr.users.verticle;

import com.ascend.flockr.common.guice.GuiceInjector;
import com.ascend.flockr.users.config.HttpServerConfig;
import com.dream11.rest.AbstractRestVerticle;
import com.dream11.rest.ClassInjector;
import com.dream11.rest.provider.JsonProvider;
import com.dream11.rest.provider.impl.JacksonProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.vertx.core.http.HttpServerOptions;

public class RestVerticle extends AbstractRestVerticle {
  private static final String PACKAGE_NAME = "com.ascend.flockr.users";

  @Inject
  public RestVerticle(HttpServerConfig httpServerConfig) {
    super(PACKAGE_NAME, getHttpServerOptions(httpServerConfig));
  }

  @Override
  protected ClassInjector getInjector() {
    return new GuiceInjector();
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
