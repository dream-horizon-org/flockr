package io.ascend.flockr.admin.client.sink.pusher;

import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.audience.AudienceRecord;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.config.KafkaSinkConfig;
import io.ascend.flockr.admin.util.ConfigurationUtil;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Sink pusher implementation for Kafka.
 *
 * <p>Pushes audience records as JSON messages to the configured Kafka topic. Each record is sent as
 * a separate message with the user ID as the key.
 */
@Slf4j
public class KafkaSinkPusher implements SinkPusher {

  private final KafkaProducer<String, String> producer;

  public KafkaSinkPusher(KafkaProducer<String, String> producer) {
    this.producer = producer;
  }

  @Override
  public Completable pushBatch(
      List<AudienceRecord> records, DataSinkDetails sink, AudienceMeta audience) {
    return Completable.fromAction(
        () -> {
          KafkaSinkConfig config =
              ConfigurationUtil.parseSinkConfig(sink.getConfig(), KafkaSinkConfig.class);

          String topic = config.getTopic();
          Long audienceId = audience.getAudienceId();

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

  @Override
  public void close() {
    try {
      producer.close();
    } catch (Exception e) {
      log.error("Error closing Kafka producer", e);
    }
  }
}
