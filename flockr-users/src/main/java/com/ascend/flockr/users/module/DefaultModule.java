package com.ascend.flockr.users.module;

import com.ascend.flockr.users.controller.BulkCohortAssignment;
import com.ascend.flockr.users.controller.GetUserCohorts;
import com.ascend.flockr.users.controller.HealthCheck;
import com.ascend.flockr.users.controller.MapUserCohorts;
import com.ascend.flockr.users.service.HealthCheckService;
import com.ascend.flockr.users.service.UserCohortsService;
import com.ascend.flockr.users.service.impl.HealthCheckServiceImpl;
import com.ascend.flockr.users.service.impl.UserCohortServiceImpl;
import com.ascend.flockr.users.verticle.RestVerticle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.vertx.core.Vertx;

// import io.vertx.rxjava3.core.Vertx as RxVertx;

/**
 * Guice module for flockr-users dependency injection configuration.
 *
 * <p>Configures bindings for services, controllers, and configuration objects used in the
 * flockr-users module.
 *
 * @since 1.0
 */
public class DefaultModule extends AbstractModule {
  private final Vertx vertx; // Changed to io.vertx.core.Vertx

  public DefaultModule(Vertx vertx) { // Now accepts io.vertx.core.Vertx
    this.vertx = vertx;
  }

  @Override
  protected void configure() {
    // Bind services
    bind(UserCohortsService.class).to(UserCohortServiceImpl.class).in(Singleton.class);
    bind(HealthCheckService.class).to(HealthCheckServiceImpl.class).in(Singleton.class);

    // Bind controllers (for REST framework scanning)
    bind(BulkCohortAssignment.class);
    bind(GetUserCohorts.class);
    bind(MapUserCohorts.class);
    bind(HealthCheck.class);

    // Bind verticle
    bind(RestVerticle.class);
  }

  @Provides
  @Singleton
  ObjectMapper provideObjectMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Provides
  @Singleton
  io.vertx.rxjava3.core.Vertx provideRxVertx() {
    return io.vertx.rxjava3.core.Vertx.newInstance(vertx);
  }
}
