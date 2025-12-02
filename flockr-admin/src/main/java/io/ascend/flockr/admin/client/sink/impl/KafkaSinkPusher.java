package io.ascend.flockr.admin.client.sink.impl;

import io.ascend.flockr.admin.client.sink.SinkPusher;
import io.ascend.flockr.admin.domain.audience.AudienceRecord;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.config.KafkaSinkConfig;
import io.ascend.flockr.admin.util.ConfigParser;
import io.reactivex.rxjava3.core.Completable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Sink pusher implementation for Kafka.
 *
 * <p>Pushes audience records as JSON messages to the configured Kafka topic. Each record is sent as
 * a separate message with the user ID as the key.
 */
@Slf4j
public class KafkaSinkPusher implements SinkPusher {

  private static final String SINK_TYPE = "KAFKA";

  // Cache producers by bootstrap servers to reuse connections
  private final Map<String, KafkaProducer<String, String>> producerCache =
      new ConcurrentHashMap<>();

  @Override
  public String getSinkType() {
    return SINK_TYPE;
  }

  @Override
  public Completable pushBatch(
      List<AudienceRecord> records, DataSinkDetails sink, Long audienceId) {
    return Completable.fromAction(
        () -> {
          KafkaSinkConfig config =
              ConfigParser.parseSinkConfig(sink.getConfig(), KafkaSinkConfig.class);
          KafkaProducer<String, String> producer =
              getOrCreateProducer(config.getBootstrapServersUrl());

          String topic = config.getTopic();

          log.debug(
              "Pushing {} records to Kafka topic '{}' for audience {}",
              records.size(),
              topic,
              audienceId);

          for (AudienceRecord record : records) {
            // Use userId as the Kafka message key for partitioning
            String key = record.getUserId();

            ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>(topic, key, record.toJson().encode());

            producer.send(
                producerRecord,
                (metadata, exception) -> {
                  if (exception != null) {
                    log.error(
                        "Failed to send record to Kafka topic {}: {}",
                        topic,
                        exception.getMessage());
                  }
                });
          }

          // Flush to ensure all records are sent
          producer.flush();

          log.info(
              "Successfully pushed {} records to Kafka topic '{}' for audience {}",
              records.size(),
              topic,
              audienceId);
        });
  }

  private KafkaProducer<String, String> getOrCreateProducer(String bootstrapServers) {
    return producerCache.computeIfAbsent(
        bootstrapServers,
        servers -> {
          Map<String, Object> props = new HashMap<>();
          props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
          props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
          props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
          props.put(ProducerConfig.ACKS_CONFIG, "1"); // Wait for leader ack
          props.put(ProducerConfig.RETRIES_CONFIG, 3);
          props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
          props.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Small delay to batch records

          log.info("Creating new Kafka producer for bootstrap servers: {}", servers);
          return new KafkaProducer<>(props);
        });
  }

  /** Closes all cached Kafka producers. Call this on application shutdown. */
  public void close() {
    producerCache.forEach(
        (servers, producer) -> {
          log.info("Closing Kafka producer for: {}", servers);
          producer.close();
        });
    producerCache.clear();
  }
}
