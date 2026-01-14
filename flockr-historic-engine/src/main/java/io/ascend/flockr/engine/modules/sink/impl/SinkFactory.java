package io.ascend.flockr.engine.modules.sink.impl;

import io.ascend.flockr.engine.config.KafkaProducerConfig;
import io.ascend.flockr.engine.config.S3Config;
import io.ascend.flockr.engine.enums.SinkTypes;
import io.ascend.flockr.engine.modules.sink.Sink;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.spark.sql.SparkSession;

public class SinkFactory {

  public static Sink<String> createSink(
      SinkTypes sinkType,
      KafkaProducer<String, String> kafkaProducer,
      KafkaProducerConfig kafkaConfig,
      S3Config s3Config,
      SparkSession sparkSession) {
    switch (sinkType) {
      case KAFKA:
        if (kafkaProducer == null || kafkaConfig == null) {
          throw new IllegalArgumentException(
              "KafkaProducer and KafkaProducerConfig are required for Kafka sink");
        }
        return new KafkaSinkImpl(kafkaProducer, kafkaConfig);
      case S3:
        if (s3Config == null || sparkSession == null) {
          throw new IllegalArgumentException("S3Config and SparkSession are required for S3 sink");
        }
        return new S3SinkImpl(s3Config, sparkSession);
      case API:
      case WEBHOOK:
        throw new UnsupportedOperationException(
            sinkType + " sinks should be created through EngineModule, not SinkFactory");
      default:
        throw new IllegalArgumentException("Invalid sink type: " + sinkType);
    }
  }
}
