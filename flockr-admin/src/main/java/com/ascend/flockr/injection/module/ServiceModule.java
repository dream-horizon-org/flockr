package com.ascend.flockr.injection.module;

import com.ascend.flockr.client.aerospike.AerospikeClient;
import com.ascend.flockr.client.aerospike.impl.AerospikeClientImpl;
import com.ascend.flockr.client.datadog.DDClient;
import com.ascend.flockr.client.datadog.impl.DDClientImpl;
import com.ascend.flockr.client.kafka.KafkaProducerClient;
import com.ascend.flockr.client.kafka.impl.KafkaProducerClientImpl;
import com.ascend.flockr.client.mysql.MySQLReaderClient;
import com.ascend.flockr.client.mysql.MySQLWriterClient;
import com.ascend.flockr.client.mysql.impl.MySQLReaderClientImpl;
import com.ascend.flockr.client.mysql.impl.MySQLWriterClientImpl;
import com.ascend.flockr.client.webclient.WebClient;
import com.ascend.flockr.client.webclient.impl.WebClientImpl;
import com.ascend.flockr.config.*;
import com.ascend.flockr.dao.*;
import com.ascend.flockr.dao.impl.*;
import com.ascend.flockr.model.task.rule.PatternSequenceRule;
import com.ascend.flockr.service.*;
import com.ascend.flockr.service.admin.AdminOperationImpl;
import com.ascend.flockr.service.flink.FlinkClientImpl;
import com.ascend.flockr.service.impl.*;
import com.ascend.flockr.service.web.AsyncJobService;
import com.ascend.flockr.util.CircuitBreakerFactory;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
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
    bind(HttpServerConfig.class).toProvider(HttpServerConfig.provider()).asEagerSingleton();
    bind(KafkaProducerConfig.class).toProvider(KafkaProducerConfig.provider()).asEagerSingleton();
    bind(MySQLConfig.class).toProvider(MySQLConfig.provider()).asEagerSingleton();
    bind(WebClientConfig.class).toProvider(WebClientConfig.provider()).asEagerSingleton();
  }

  private void bindClients() {
    bind(AerospikeClientImpl.class).in(Singleton.class);
    bind(AerospikeClient.class).to(AerospikeClientImpl.class);
    bind(DDClientImpl.class).in(Singleton.class);
    bind(DDClient.class).to(DDClientImpl.class);
    bind(KafkaProducerClientImpl.class).in(Singleton.class);
    bind(KafkaProducerClient.class).to(KafkaProducerClientImpl.class);
    bind(MySQLReaderClientImpl.class).in(Singleton.class);
    bind(MySQLWriterClientImpl.class).in(Singleton.class);
    bind(MySQLReaderClient.class).to(MySQLReaderClientImpl.class);
    bind(MySQLWriterClient.class).to(MySQLWriterClientImpl.class);
    bind(WebClientImpl.class).in(Singleton.class);
    bind(WebClient.class).to(WebClientImpl.class);
    bind(FlinkClient.class).to(FlinkClientImpl.class);
//    bind(new TypeLiteral<AsyncJobService<PatternSequenceRule>>() {})
//        .to(EventStreamJobService.class);
    bind(AdminOperation.class).to(AdminOperationImpl.class);
  }

  private void bindDAOs() {
    bind(HealthCheckDAO.class).to(HealthCheckDAOImpl.class);
  }

  private void bindServices() {
    bind(HealthCheckService.class).to(HealthCheckServiceImpl.class);
  }
}
