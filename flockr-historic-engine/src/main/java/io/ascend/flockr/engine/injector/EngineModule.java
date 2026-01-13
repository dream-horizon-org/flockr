package io.ascend.flockr.engine.injector;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import io.ascend.flockr.engine.config.ApiConfig;
import io.ascend.flockr.engine.config.AthenaConfig;
import io.ascend.flockr.engine.config.ConnectorConfig;
import io.ascend.flockr.engine.config.KafkaConfig;
import io.ascend.flockr.engine.config.KafkaProducerConfig;
import io.ascend.flockr.engine.config.S3Config;
import io.ascend.flockr.engine.constants.Constants;
import io.ascend.flockr.engine.enums.SourceTypes;
import io.ascend.flockr.engine.modules.sink.Sink;
import io.ascend.flockr.engine.modules.sink.impl.ApiSinkImpl;
import io.ascend.flockr.engine.modules.sink.impl.KafkaSinkImpl;
import io.ascend.flockr.engine.modules.sink.impl.S3SinkImpl;
import io.ascend.flockr.engine.modules.source.Source;
import io.ascend.flockr.engine.modules.source.impl.SourceFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Guice dependency injection module for the Flocker Historic Engine.
 *
 * <p>This module configures dependency injection for:
 *
 * <ul>
 *   <li><b>Source</b>: Provides a single Source instance based on source configuration
 *   <li><b>Sinks</b>: Provides a list of Sink instances based on sink configurations
 *   <li><b>SparkSession</b>: Binds the Spark session as a singleton
 * </ul>
 *
 * <p><b>Source Creation:</b>
 *
 * <p>The module creates Source instances using the {@link
 * io.ascend.flockr.engine.modules.source.impl.SourceFactory} based on the source type (ATHENA, S3,
 * KAFKA, etc.).
 *
 * <p><b>Sink Creation:</b>
 *
 * <p>The module creates Sink instances directly:
 *
 * <ul>
 *   <li><b>S3</b>: Creates S3SinkImpl with configured S3 path and write mode
 *   <li><b>API</b>: Creates ApiSinkImpl with API configuration and audience metadata
 *   <li><b>KAFKA</b>: Currently not implemented (logs warning and skips)
 * </ul>
 *
 * <p><b>Configuration Handling:</b>
 *
 * <p>The module handles both typed config objects (e.g., S3Config, ApiConfig) and raw Typesafe
 * Config objects, converting them as needed.
 *
 * @see com.google.inject.AbstractModule
 * @see Source
 * @see Sink
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class EngineModule extends AbstractModule {

  /** Spark session for distributed processing. */
  private final SparkSession sparkSession;

  /** Single source connector configuration. */
  private final ConnectorConfig sourceConfig;

  /** List of sink connector configurations. */
  private final List<ConnectorConfig> sinkConfigs;

  /** Audience/cohort name (used for API sinks). */
  @SuppressWarnings("unused")
  private final String audienceName;

  /** Action type ("append" or "remove", used for API sinks). */
  @SuppressWarnings("unused")
  private final String action;

  /** Expiration timestamp (used for API sinks). */
  @SuppressWarnings("unused")
  private final Long expireAt;

  /**
   * Creates a new EngineModule with the specified configurations.
   *
   * @param sparkSession The Spark session instance.
   * @param sourceConfig Single source connector configuration.
   * @param sinkConfigs List of sink connector configurations.
   * @param audienceName The audience/cohort name.
   * @param action The action type ("append" or "remove").
   * @param expireAt The expiration timestamp in epoch seconds.
   */
  public EngineModule(
      SparkSession sparkSession,
      ConnectorConfig sourceConfig,
      List<ConnectorConfig> sinkConfigs,
      String audienceName,
      String action,
      Long expireAt) {
    this.sparkSession = sparkSession;
    this.sourceConfig = sourceConfig;
    this.sinkConfigs = sinkConfigs;
    this.audienceName = audienceName;
    this.action = action;
    this.expireAt = expireAt;
    log.debug("EngineModule initialized with action: {}, expireAt: {}", action, expireAt);
  }

  @Override
  protected void configure() {
    bind(SparkSession.class).toInstance(sparkSession);
  }

  /**
   * Provides a single Source instance based on source configuration.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Converts raw Config objects to typed config objects if needed
   *   <li>Creates Source instance using SourceFactory
   *   <li>Returns the source as a singleton
   * </ol>
   *
   * <p><b>Supported Source Types:</b>
   *
   * <ul>
   *   <li>ATHENA → AthenaSourceImpl
   *   <li>S3 → S3SourceImpl
   *   <li>KAFKA → KafkaSourceImpl (if implemented)
   * </ul>
   *
   * @return A singleton Source instance.
   * @throws IllegalArgumentException If an unsupported source type is encountered.
   */
  @Provides
  @Singleton
  @Named("source")
  public Source<Dataset<Row>> provideSource() {
    log.debug("Providing Source instance");
    SourceTypes type = SourceTypes.valueOf(sourceConfig.getType().toUpperCase());
    Object configObj = sourceConfig.getConfig();

    if (configObj instanceof Config) {
      Config typesafeConfig = (Config) configObj;
      configObj =
          switch (type) {
            case S3 -> S3Config.fromConfig(typesafeConfig);
            case ATHENA -> AthenaConfig.fromConfig(typesafeConfig);
            case KAFKA -> KafkaConfig.fromConfig(typesafeConfig);
            default -> throw new IllegalArgumentException("Unsupported source type: " + type);
          };
    }

    return SourceFactory.createSource(type, configObj, sparkSession);
  }

  /**
   * Provides a list of Sink instances based on sink configurations.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Iterates through sink configurations
   *   <li>Converts raw Config objects to typed config objects if needed
   *   <li>Creates Sink instances based on sink type
   *   <li>Returns the list of sinks as a singleton
   * </ol>
   *
   * <p><b>Supported Sink Types:</b>
   *
   * <ul>
   *   <li><b>S3</b>: Creates S3SinkImpl with S3 path and write mode
   *   <li><b>API</b>: Creates ApiSinkImpl with API config and audience metadata
   *   <li><b>KAFKA</b>: Currently not implemented (logs warning and skips)
   * </ul>
   *
   * <p><b>S3 Sink Configuration:</b>
   *
   * <p>The S3 sink uses the configured bucket and path to construct the S3 URI. Write mode defaults
   * to "append" if not specified.
   *
   * <p><b>API Sink Configuration:</b>
   *
   * <p>The API sink is configured with the API config, audience name, action, and expiration
   * timestamp for building request payloads.
   *
   * @return A singleton list of Sink instances.
   * @throws IllegalArgumentException If an unknown config type is encountered.
   */
  @Provides
  @Singleton
  @Named("sinks")
  public List<Sink<String>> provideSinks() {
    log.debug("Providing Sink instances");
    List<Sink<String>> sinks = new ArrayList<>();

    for (ConnectorConfig config : sinkConfigs) {
      String type = config.getType().toUpperCase();
      if ("S3".equals(type)) {
        S3Config s3Config;
        Object configObj = config.getConfig();

        if (configObj instanceof S3Config) {
          s3Config = (S3Config) configObj;
        } else if (configObj instanceof Config) {
          s3Config = S3Config.fromConfig((Config) configObj);
        } else {
          throw new IllegalArgumentException(
              "Unknown config type for S3 sink: " + configObj.getClass().getName());
        }

        String writeMode =
            s3Config.getWriteMode() != null ? s3Config.getWriteMode() : Constants.WRITE_MODE_APPEND;
        String outputPath = "s3a://" + s3Config.getBucket() + "/" + s3Config.getPath();

        sinks.add(new S3SinkImpl(s3Config, sparkSession, writeMode, outputPath));
        log.info("Added S3 sink: {}", outputPath);
      } else if ("WEBHOOK".equals(type)) {
        Object configObj = config.getConfig();
        ApiConfig apiConfig;

        if (configObj instanceof ApiConfig) {
          apiConfig = (ApiConfig) configObj;
        } else if (configObj instanceof Config) {
          apiConfig = ApiConfig.fromConfig((Config) configObj);
        } else {
          throw new IllegalArgumentException(
              "Unknown config type for API sink: " + configObj.getClass().getName());
        }

        sinks.add(new ApiSinkImpl(apiConfig, audienceName, action, expireAt));
        log.info("Added API sink with rate limit: {}/sec", apiConfig.getRateLimitPerSecond());
      } else if ("KAFKA".equals(type)) {
        Object configObj = config.getConfig();
        KafkaConfig kafkaConfig;

        if (configObj instanceof KafkaConfig) {
          kafkaConfig = (KafkaConfig) configObj;
        } else if (configObj instanceof Config) {
          kafkaConfig = KafkaConfig.fromConfig((Config) configObj);
        } else {
          throw new IllegalArgumentException(
              "Unknown config type for Kafka sink: " + configObj.getClass().getName());
        }

        // Create KafkaProducerConfig from KafkaConfig
        KafkaProducerConfig producerConfig = new KafkaProducerConfig();
        producerConfig.setBootstrapServers(kafkaConfig.getBootstrapServersUrl());
        producerConfig.setTopic(kafkaConfig.getTopic());
        producerConfig.setKeySerializerClass(
            "org.apache.kafka.common.serialization.StringSerializer");
        producerConfig.setValueSerializerClass(
            "org.apache.kafka.common.serialization.StringSerializer");

        // Create KafkaProducer
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerConfig.getBootstrapServers());
        props.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerConfig.getKeySerializerClass());
        props.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerConfig.getValueSerializerClass());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);
        sinks.add(new KafkaSinkImpl(kafkaProducer, producerConfig));
        log.info(
            "Added Kafka sink for topic: {} at bootstrap servers: {}",
            producerConfig.getTopic(),
            producerConfig.getBootstrapServers());
      }
    }

    return sinks;
  }
}
