package io.ascend.flockr.admin.client.sink.factory;

import io.ascend.flockr.admin.client.sink.pusher.KafkaSinkPusher;
import io.ascend.flockr.admin.client.sink.pusher.SinkPusher;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.config.KafkaSinkConfig;
import io.ascend.flockr.admin.util.ConfigurationUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

@Slf4j
public class KafkaSinkPusherFactory implements SinkPusherFactory {
  private static final String SINK_TYPE = "KAFKA";

  private final Map<String, KafkaSinkPusher> pusherCache = new ConcurrentHashMap<>();

  @Override
  public String getSinkType() {
    return SINK_TYPE;
  }

  @Override
  public SinkPusher create(DataSinkDetails sink) {
    KafkaSinkConfig config =
        ConfigurationUtil.parseSinkConfig(sink.getConfig(), KafkaSinkConfig.class);
    return getOrCreatePusher(config.getBootstrapServersUrl());
  }

  private KafkaSinkPusher getOrCreatePusher(String bootstrapServers) {
    return pusherCache.computeIfAbsent(
        bootstrapServers,
        servers -> {
          Map<String, Object> props = new HashMap<>();
          props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
          props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
          props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
          props.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for leader ack
          props.put(ProducerConfig.RETRIES_CONFIG, 3);
          props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
          props.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Small delay to batch records

          log.info("Creating new Kafka pusher for bootstrap servers: {}", servers);
          return new KafkaSinkPusher(new KafkaProducer<>(props));
        });
  }

  /** Closes all cached Kafka pushers. Call this on application shutdown. */
  public void close() {
    pusherCache.forEach(
        (servers, pusher) -> {
          log.info("Closing Kafka pusher for: {}", servers);
          pusher.close();
        });
    pusherCache.clear();
  }
}
