package io.ascend.flockr.admin.injection.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.ascend.flockr.admin.client.flink.FlinkClient;
import io.ascend.flockr.admin.client.flink.FlinkClientImpl;
import io.ascend.flockr.admin.client.postgres.PostgresReaderClient;
import io.ascend.flockr.admin.client.postgres.PostgresWriterClient;
import io.ascend.flockr.admin.client.postgres.impl.PostgresReaderClientImpl;
import io.ascend.flockr.admin.client.postgres.impl.PostgresWriterClientImpl;
import io.ascend.flockr.admin.client.sink.SinkPusherRegistry;
import io.ascend.flockr.admin.client.sink.factory.KafkaSinkPusherFactory;
import io.ascend.flockr.admin.client.sink.factory.S3SinkPusherFactory;
import io.ascend.flockr.admin.client.sink.factory.SinkPusherFactory;
import io.ascend.flockr.admin.client.sink.factory.WebhookSinkPusherFactory;
import io.ascend.flockr.admin.client.spark.SparkClient;
import io.ascend.flockr.admin.client.spark.SparkClientImpl;
import io.ascend.flockr.admin.client.webclient.WebClient;
import io.ascend.flockr.admin.client.webclient.impl.WebClientImpl;
import io.ascend.flockr.admin.config.*;
import io.ascend.flockr.admin.handlers.AbstractHandler;
import io.ascend.flockr.admin.handlers.ExecuteRuleHandler;
import io.ascend.flockr.admin.handlers.ReconcileJobHandler;
import io.ascend.flockr.admin.repository.*;
import io.ascend.flockr.admin.repository.ExecutionSync;
import io.ascend.flockr.admin.repository.impl.*;
import io.ascend.flockr.admin.repository.impl.PostgresExecutionSync;
import io.ascend.flockr.admin.service.*;
import io.ascend.flockr.admin.service.impl.*;
import io.ascend.flockr.admin.util.AsyncJakartaValidationUtil;
import io.ascend.flockr.admin.util.CircuitBreakerFactory;
import io.ascend.flockr.admin.util.ConfigurationUtil;
import io.ascend.flockr.admin.util.JsonUtil;
import io.vertx.rxjava3.core.Vertx;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

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
 * @author Prithu Sharma
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
    /* Bind Utilities */
    bindUtilities();
    /* Bind DAOs */
    bindDAOs();
    /* Bind Services */
    bindServices();
    /* Static Binding */
    requestStaticInjection(
        ConfigurationUtil.class,
        JsonUtil.class,
        AsyncJakartaValidationUtil.class,
        CircuitBreakerFactory.class); // ← Add static injection

    bindSchedulers();
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
    bind(SparkConfig.class).toProvider(SparkConfig.provider()).asEagerSingleton();
    bind(EncryptionConfig.class).asEagerSingleton();
  }

  /**
   * Binds client implementations as singletons.
   *
   * <p>Binds WebClient, FlinkClient, PostgreSQL reader/writer clients, and sink pusher factories.
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
    //    sink pusher factory bindings
    bind(KafkaSinkPusherFactory.class).in(Singleton.class);
    bind(S3SinkPusherFactory.class).in(Singleton.class);
    bind(WebhookSinkPusherFactory.class).in(Singleton.class);

    bind(SparkClientImpl.class).in(Singleton.class);
    bind(SparkClient.class).to(SparkClientImpl.class);
  }

  /**
   * Provides the SinkPusherRegistry with all available sink pusher factories.
   *
   * @param kafkaFactory the Kafka sink pusher factory
   * @param s3Factory the S3 sink pusher factory
   * @param webhookFactory the Webhook sink pusher factory
   * @return the configured SinkPusherRegistry
   */
  @Provides
  @Singleton
  SinkPusherRegistry provideSinkPusherRegistry(
      KafkaSinkPusherFactory kafkaFactory,
      S3SinkPusherFactory s3Factory,
      WebhookSinkPusherFactory webhookFactory) {
    Set<SinkPusherFactory> factories = Set.of(kafkaFactory, s3Factory, webhookFactory);
    return new SinkPusherRegistry(factories);
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
    bind(RuleExecutionRepository.class).to(RuleExecutionRepositoryImpl.class);
  }

  /**
   * Binds utility classes as singletons.
   *
   * <p>Binds JSON schema validation utilities and Jakarta Bean Validator for async validation.
   */
  private void bindUtilities() {
    bind(JsonSchemaFactory.class)
        .toInstance(
            JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4))
                .build());

    bind(Validator.class).toInstance(Validation.buildDefaultValidatorFactory().getValidator());
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
    bind(AudienceImportService.class).to(AudienceImportServiceImpl.class);
    bind(BatchRuleExecutionService.class).in(Singleton.class);
    bind(JobServiceRegistry.class).in(Singleton.class);
  }

  /**
   * Binds scheduler-related components.
   *
   * <p>Binds the distributed execution synchronization implementation and all scheduled handlers.
   */
  private void bindSchedulers() {
    bind(PostgresExecutionSync.class).in(Singleton.class);
    bind(ExecutionSync.class).to(PostgresExecutionSync.class);

    // Bind handlers using Multibinder for Set injection
    Multibinder<AbstractHandler> handlerBinder =
        Multibinder.newSetBinder(binder(), AbstractHandler.class);
    handlerBinder.addBinding().to(ExecuteRuleHandler.class).in(Singleton.class);
    handlerBinder.addBinding().to(ReconcileJobHandler.class).in(Singleton.class);
  }
}
