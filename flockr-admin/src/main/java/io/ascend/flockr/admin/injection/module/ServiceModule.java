package io.ascend.flockr.admin.injection.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.ascend.flockr.admin.client.flink.FlinkClient;
import io.ascend.flockr.admin.client.flink.impl.FlinkClientImpl;
import io.ascend.flockr.admin.client.postgres.PostgresReaderClient;
import io.ascend.flockr.admin.client.postgres.PostgresWriterClient;
import io.ascend.flockr.admin.client.postgres.impl.PostgresReaderClientImpl;
import io.ascend.flockr.admin.client.postgres.impl.PostgresWriterClientImpl;
import io.ascend.flockr.admin.client.sink.SinkPusher;
import io.ascend.flockr.admin.client.sink.SinkPusherRegistry;
import io.ascend.flockr.admin.client.sink.impl.KafkaSinkPusher;
import io.ascend.flockr.admin.client.sink.impl.S3SinkPusher;
import io.ascend.flockr.admin.client.sink.impl.WebhookSinkPusher;
import io.ascend.flockr.admin.client.webclient.WebClient;
import io.ascend.flockr.admin.client.webclient.impl.WebClientImpl;
import io.ascend.flockr.admin.config.*;
import io.ascend.flockr.admin.repository.*;
import io.ascend.flockr.admin.repository.impl.*;
import io.ascend.flockr.admin.service.*;
import io.ascend.flockr.admin.service.impl.*;
import io.ascend.flockr.admin.util.AsyncJakartaValidationUtil;
import io.ascend.flockr.admin.util.CircuitBreakerFactory;
import io.ascend.flockr.admin.util.ConfigParser;
import io.ascend.flockr.admin.util.json.JsonSchemaValidationUtil;
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
        ConfigParser.class,
        JsonSchemaValidationUtil.class,
        AsyncJakartaValidationUtil.class,
        CircuitBreakerFactory.class); // ← Add static injection
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
   * <p>Binds WebClient, FlinkClient, PostgreSQL reader/writer clients, and sink pushers.
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
    //    sink pusher bindings
    bind(KafkaSinkPusher.class).in(Singleton.class);
    bind(S3SinkPusher.class).in(Singleton.class);
    bind(WebhookSinkPusher.class).in(Singleton.class);
  }

  /**
   * Provides the SinkPusherRegistry with all available sink pushers.
   *
   * @param kafkaPusher the Kafka sink pusher
   * @param s3Pusher the S3 sink pusher
   * @param webhookPusher the Webhook sink pusher
   * @return the configured SinkPusherRegistry
   */
  @Provides
  @Singleton
  SinkPusherRegistry provideSinkPusherRegistry(
      KafkaSinkPusher kafkaPusher, S3SinkPusher s3Pusher, WebhookSinkPusher webhookPusher) {
    Set<SinkPusher> pushers = Set.of(kafkaPusher, s3Pusher, webhookPusher);
    return new SinkPusherRegistry(pushers);
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
   * Binds utility classes as singletons.
   *
   * <p>Binds JSON schema validation utilities and Jakarta Bean Validator for async validation.
   */
  private void bindUtilities() {
    bind(JsonSchemaFactory.class)
        .toInstance(
            JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4))
                .build());

    // Bind Jakarta Validator for static injection into AsyncValidationUtil
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
  }
}
