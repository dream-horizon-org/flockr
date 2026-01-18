package io.ascend.flockr.engine.modules.sink.impl;

import static org.apache.spark.sql.functions.lit;

import io.ascend.flockr.engine.config.KafkaProducerConfig;
import io.ascend.flockr.engine.constants.Constants;
import io.ascend.flockr.engine.dto.AudienceMetadata;
import io.ascend.flockr.engine.dto.UserIdRow;
import io.ascend.flockr.engine.exception.JobException;
import io.ascend.flockr.engine.modules.sink.Sink;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

@Slf4j
public class KafkaSinkImpl implements Sink {

  private final KafkaProducerConfig kafkaConfig;

  public KafkaSinkImpl(KafkaProducerConfig kafkaConfig) {
    if (kafkaConfig == null) {
      throw new IllegalArgumentException("KafkaProducerConfig cannot be null");
    }
    if (kafkaConfig.getTopic() == null || kafkaConfig.getTopic().isEmpty()) {
      throw new IllegalArgumentException("Kafka topic cannot be null or empty");
    }
    this.kafkaConfig = kafkaConfig;
  }

  @Override
  public void write(Dataset<UserIdRow> userIds, AudienceMetadata metadata) {
    log.info("Writing dataset to Kafka topic: {}", kafkaConfig.getTopic());

    final KafkaProducerConfig config = this.kafkaConfig;
    Dataset<Row> dataWithMetadata =
        userIds
            .toDF()
            .withColumn(Constants.AUDIENCE_NAME_COLUMN, lit(metadata.getAudienceName()))
            .withColumn(Constants.ACTION_COLUMN, lit(metadata.getAction()))
            .withColumn(Constants.EXPIRE_AT_COLUMN, lit(metadata.getExpireAt()));

    dataWithMetadata.foreachPartition(
        iterator -> {
          if (!iterator.hasNext()) {
            log.info("No records to process in this partition");
            return;
          }
          Properties props = createKafkaProducerProperties(config);
          try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            int successCount = 0;
            int errorCount = 0;
            List<Future<RecordMetadata>> futures = new ArrayList<>();
            while (iterator.hasNext()) {
              Row row = iterator.next();
              try {
                String json = row.json();
                ProducerRecord<String, String> record =
                    new ProducerRecord<>(config.getTopic(), json);
                futures.add(producer.send(record));
              } catch (Exception e) {
                log.error("Error converting row to JSON or sending to Kafka", e);
                errorCount++;
              }
            }
            for (Future<RecordMetadata> future : futures) {
              try {
                future.get(); // Blocks until acknowledged, throws on failure
                successCount++;
              } catch (Exception e) {
                log.error("Kafka send failed for message", e);
                errorCount++;
              }
            }
            producer.flush();
            log.info(
                "Successfully sent {} messages from partition to Kafka topic: {}, errors: {}",
                successCount,
                config.getTopic(),
                errorCount);

            if (errorCount > 0) {
              throw new JobException(
                  "Failed to send " + errorCount + " messages to Kafka in partition");
            }
          }
        });

    log.info("Successfully sent data to Kafka topic: {}", kafkaConfig.getTopic());
  }

  /**
   * Creates Kafka producer properties with optimized settings.
   *
   * <p>This static method is used within the foreachPartition lambda to create Properties for
   * KafkaProducer instances on each partition.
   *
   * @param producerConfig The Kafka producer configuration.
   * @return Properties configured for Kafka producer.
   */
  private static Properties createKafkaProducerProperties(KafkaProducerConfig producerConfig) {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerConfig.getBootstrapServers());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerConfig.getKeySerializerClass());
    props.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerConfig.getValueSerializerClass());
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 3);
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
    props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    return props;
  }
}
