package com.ascend.flockr.injection.module;

import com.ascend.flockr.client.flink.FlinkClient;
import com.ascend.flockr.client.flink.impl.FlinkClientImpl;
import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.client.postgres.PostgresWriterClient;
import com.ascend.flockr.client.postgres.impl.PostgresReaderClientImpl;
import com.ascend.flockr.client.postgres.impl.PostgresWriterClientImpl;
import com.ascend.flockr.client.webclient.WebClient;
import com.ascend.flockr.client.webclient.impl.WebClientImpl;
import com.ascend.flockr.config.*;
import com.ascend.flockr.repository.*;
import com.ascend.flockr.repository.impl.*;
import com.ascend.flockr.service.*;
import com.ascend.flockr.service.impl.*;
import com.ascend.flockr.util.CircuitBreakerFactory;
import com.ascend.flockr.util.ConfigValidator;
import com.ascend.flockr.util.ConfigValidatorRegistry;
import com.ascend.flockr.util.validator.AthenaConfigValidator;
import com.ascend.flockr.util.validator.KafkaConfigValidator;
import com.ascend.flockr.util.validator.S3FolderSinkValidator;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.vertx.rxjava3.core.Vertx;

/**
 * Guice module that configures all service bindings for the Flockr application.
 *
 * <p>This module binds:
 *
 * <ul>
 *   <li>Configuration providers (ApplicationConfig, CircuitBreakerConfig, etc.)
 *   <li>Client implementations (WebClient, FlinkClient, PostgreSQL clients)
 *   <li>Repository implementations
 *   <li>Config validators (using Multibinder for extensibility)
 *   <li>Service implementations
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
public class ServiceModule extends DefaultModule {
  /**
   * Constructs a new ServiceModule with the provided Vert.x instance.
   *
   * @param vertx the Vert.x instance to use for module configuration
   */
  public ServiceModule(Vertx vertx) {
    super(vertx);
  }

  /**
   * Configures all bindings for the application.
   *
   * <p>This method sets up bindings in the following order:
   *
   * <ol>
   *   <li>Configuration providers
   *   <li>Client implementations
   *   <li>Repository implementations
   *   <li>Config validators
   *   <li>Service implementations
   *   <li>Static injection for CircuitBreakerFactory
   * </ol>
   */
  @Override
  protected void configure() {
    super.configure();
    /* Bind Configs */
    bindConfigs();
    /* Bind Clients */
    bindClients();
    /* Bind DAOs */
    bindDAOs();
    /* Bind Validators */
    bindValidators();
    /* Bind Services */
    bindServices();
    /* Static Binding */
    requestStaticInjection(CircuitBreakerFactory.class);
  }

  /**
   * Binds configuration providers as eager singletons.
   *
   * <p>All configuration classes are bound as eager singletons to ensure they are loaded at
   * application startup.
   */
  private void bindConfigs() {
    bind(ApplicationConfig.class).toProvider(ApplicationConfig.provider()).asEagerSingleton();
    bind(CircuitBreakerConfig.class).toProvider(CircuitBreakerConfig.provider()).asEagerSingleton();
    bind(FlinkConfig.class).toProvider(FlinkConfig.provider()).asEagerSingleton();
    bind(HttpServerConfig.class).toProvider(HttpServerConfig.provider()).asEagerSingleton();
    bind(PostgresConfig.class).toProvider(PostgresConfig.provider()).asEagerSingleton();
    bind(WebClientConfig.class).toProvider(WebClientConfig.provider()).asEagerSingleton();
  }

  /**
   * Binds client implementations as singletons.
   *
   * <p>Binds WebClient, FlinkClient, and PostgreSQL reader/writer clients.
   */
  private void bindClients() {
    //    web client bindings
    bind(WebClientImpl.class).in(Singleton.class);
    bind(WebClient.class).to(WebClientImpl.class);
    //    flink client bindings
    bind(FlinkClientImpl.class).in(Singleton.class);
    bind(FlinkClient.class).to(FlinkClientImpl.class);
    //    postgres client bindings
    bind(PostgresReaderClientImpl.class).in(Singleton.class);
    bind(PostgresWriterClientImpl.class).in(Singleton.class);
    bind(PostgresReaderClient.class).to(PostgresReaderClientImpl.class);
    bind(PostgresWriterClient.class).to(PostgresWriterClientImpl.class);
  }

  /**
   * Binds repository implementations.
   *
   * <p>Binds all repository interfaces to their implementations.
   */
  private void bindDAOs() {
    bind(HealthCheckDAO.class).to(HealthCheckDAOImpl.class);
    bind(DataConnectorRepository.class).to(DataConnectorRepositoryImpl.class);
    bind(AudienceRepository.class).to(AudienceRepositoryImpl.class);
    bind(RuleRepository.class).to(RuleRepositoryImpl.class);
  }

  /**
   * Binds config validators using Multibinder for extensibility.
   *
   * <p>Uses Guice Multibinder to collect all ConfigValidator implementations, allowing new
   * validators to be added by simply binding them to the ConfigValidator interface.
   */
  private void bindValidators() {
    // Use Multibinder to collect all ConfigValidator implementations
    Multibinder<ConfigValidator> multibinder =
        Multibinder.newSetBinder(binder(), ConfigValidator.class);

    // Bind individual validators
    multibinder.addBinding().to(AthenaConfigValidator.class).in(Singleton.class);
    multibinder.addBinding().to(KafkaConfigValidator.class).in(Singleton.class);
    multibinder.addBinding().to(S3FolderSinkValidator.class).in(Singleton.class);

    // Bind registry (will automatically receive Set<ConfigValidator> via injection)
    bind(ConfigValidatorRegistry.class).in(Singleton.class);
  }

  /**
   * Binds service implementations.
   *
   * <p>Binds all service interfaces to their implementations.
   */
  private void bindServices() {
    bind(HealthCheckService.class).to(HealthCheckServiceImpl.class);
    bind(DataConnectorService.class).to(DataConnectorServiceImpl.class);
    bind(AudienceService.class).to(AudienceServiceImpl.class);
  }
}
