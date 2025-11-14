package com.ascend.flockr.injection.module;

import com.ascend.flockr.client.aerospike.AerospikeClient;
import com.ascend.flockr.client.aerospike.impl.AerospikeClientImpl;
import com.ascend.flockr.client.datadog.DDClient;
import com.ascend.flockr.client.datadog.impl.DDClientImpl;
import com.ascend.flockr.client.flink.FlinkClient;
import com.ascend.flockr.client.flink.impl.FlinkClientImpl;
import com.ascend.flockr.client.kafka.KafkaProducerClient;
import com.ascend.flockr.client.kafka.impl.KafkaProducerClientImpl;
import com.ascend.flockr.client.mysql.MySQLReaderClient;
import com.ascend.flockr.client.mysql.MySQLWriterClient;
import com.ascend.flockr.client.mysql.impl.MySQLReaderClientImpl;
import com.ascend.flockr.client.mysql.impl.MySQLWriterClientImpl;
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
import com.google.inject.Singleton;
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
    /* Bind Services */
    bindServices();
    /* Static Binding */
    requestStaticInjection(CircuitBreakerFactory.class);
  }

  private void bindConfigs() {
    bind(AerospikeConfig.class).toProvider(AerospikeConfig.provider()).asEagerSingleton();
    bind(ApplicationConfig.class).toProvider(ApplicationConfig.provider()).asEagerSingleton();
    bind(CircuitBreakerConfig.class).toProvider(CircuitBreakerConfig.provider()).asEagerSingleton();
    bind(FlinkConfig.class).toProvider(FlinkConfig.provider()).asEagerSingleton();
    bind(HttpServerConfig.class).toProvider(HttpServerConfig.provider()).asEagerSingleton();
    bind(KafkaProducerConfig.class).toProvider(KafkaProducerConfig.provider()).asEagerSingleton();
    bind(MySQLConfig.class).toProvider(MySQLConfig.provider()).asEagerSingleton();
    bind(PostgresConfig.class).toProvider(PostgresConfig.provider()).asEagerSingleton();
    bind(WebClientConfig.class).toProvider(WebClientConfig.provider()).asEagerSingleton();
  }

  private void bindClients() {
    bind(AerospikeClientImpl.class).in(Singleton.class);
    bind(AerospikeClient.class).to(AerospikeClientImpl.class);
    bind(DDClientImpl.class).in(Singleton.class);
    bind(DDClient.class).to(DDClientImpl.class);
    //    flink client bindings
    bind(FlinkClientImpl.class).in(Singleton.class);
    bind(FlinkClient.class).to(FlinkClientImpl.class);
    //    kafka clients binding
    bind(KafkaProducerClientImpl.class).in(Singleton.class);
    bind(KafkaProducerClient.class).to(KafkaProducerClientImpl.class);
    //    mysql client bindings
    bind(MySQLReaderClientImpl.class).in(Singleton.class);
    bind(MySQLWriterClientImpl.class).in(Singleton.class);
    bind(MySQLReaderClient.class).to(MySQLReaderClientImpl.class);
    bind(MySQLWriterClient.class).to(MySQLWriterClientImpl.class);
    //    postgres client bindings
    bind(PostgresReaderClient.class).in(Singleton.class);
    bind(PostgresWriterClient.class).in(Singleton.class);
    bind(PostgresReaderClient.class).to(PostgresReaderClientImpl.class);
    bind(PostgresWriterClient.class).to(PostgresWriterClientImpl.class);
    //    web client bindings
    bind(WebClientImpl.class).in(Singleton.class);
    bind(WebClient.class).to(WebClientImpl.class);
  }

  private void bindDAOs() {
    bind(HealthCheckDAO.class).to(HealthCheckDAOImpl.class);
    bind(DataConnectorRepository.class).to(DataConnectorRepositoryImpl.class);
  }

  private void bindServices() {
    bind(HealthCheckService.class).to(HealthCheckServiceImpl.class);
    bind(DataConnectorService.class).to(DataConnectorServiceImpl.class);
  }
}
