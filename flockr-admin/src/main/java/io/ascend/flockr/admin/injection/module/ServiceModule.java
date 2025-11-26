package io.ascend.flockr.admin.injection.module;

import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.ascend.flockr.admin.client.flink.FlinkClient;
import io.ascend.flockr.admin.client.flink.impl.FlinkClientImpl;
import io.ascend.flockr.admin.client.postgres.PostgresReaderClient;
import io.ascend.flockr.admin.client.postgres.PostgresWriterClient;
import io.ascend.flockr.admin.client.postgres.impl.PostgresReaderClientImpl;
import io.ascend.flockr.admin.client.postgres.impl.PostgresWriterClientImpl;
import io.ascend.flockr.admin.client.webclient.WebClient;
import io.ascend.flockr.admin.client.webclient.impl.WebClientImpl;
import io.ascend.flockr.admin.config.*;
import io.ascend.flockr.admin.repository.*;
import io.ascend.flockr.admin.repository.impl.*;
import io.ascend.flockr.admin.service.*;
import io.ascend.flockr.admin.service.impl.*;
import io.ascend.flockr.admin.util.CircuitBreakerFactory;
import io.ascend.flockr.admin.util.json.JsonSchemaValidationUtil;
import io.ascend.flockr.admin.util.validator.AthenaConfigValidator;
import io.ascend.flockr.admin.util.validator.ConfigValidator;
import io.ascend.flockr.admin.util.validator.ConfigValidatorRegistry;
import io.ascend.flockr.admin.util.validator.KafkaConfigValidator;
import io.ascend.flockr.admin.util.validator.S3FolderSinkValidator;
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
    /* Bind Utilities */
    bindUtilities();
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
    bind(AudienceOwnerRepository.class).to(AudienceOwnerRepositoryImpl.class);
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
   * Binds utility classes as singletons.
   *
   * <p>Binds JSON schema validation utilities.
   */
  private void bindUtilities() {
    bind(JsonSchemaValidationUtil.class).in(Singleton.class);
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
