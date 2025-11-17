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

public class ServiceModule extends DefaultModule {
  public ServiceModule(Vertx vertx) {
    super(vertx);
  }

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

  private void bindConfigs() {
    bind(ApplicationConfig.class).toProvider(ApplicationConfig.provider()).asEagerSingleton();
    bind(CircuitBreakerConfig.class).toProvider(CircuitBreakerConfig.provider()).asEagerSingleton();
    bind(FlinkConfig.class).toProvider(FlinkConfig.provider()).asEagerSingleton();
    bind(HttpServerConfig.class).toProvider(HttpServerConfig.provider()).asEagerSingleton();
    bind(PostgresConfig.class).toProvider(PostgresConfig.provider()).asEagerSingleton();
    bind(WebClientConfig.class).toProvider(WebClientConfig.provider()).asEagerSingleton();
  }

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

  private void bindDAOs() {
    bind(HealthCheckDAO.class).to(HealthCheckDAOImpl.class);
    bind(DataConnectorRepository.class).to(DataConnectorRepositoryImpl.class);
    bind(AudienceRepository.class).to(AudienceRepositoryImpl.class);
    bind(RuleRepository.class).to(RuleRepositoryImpl.class);
  }

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

  private void bindServices() {
    bind(HealthCheckService.class).to(HealthCheckServiceImpl.class);
    bind(DataConnectorService.class).to(DataConnectorServiceImpl.class);
    bind(AudienceService.class).to(AudienceServiceImpl.class);
  }
}
