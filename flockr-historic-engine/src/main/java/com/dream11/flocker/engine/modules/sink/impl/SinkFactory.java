package com.dream11.flocker.engine.modules.sink.impl;

import com.dream11.flocker.engine.config.KafkaProducerConfig;
import com.dream11.flocker.engine.config.S3Config;
import com.dream11.flocker.engine.enums.SinkTypes;
import com.dream11.flocker.engine.modules.sink.Sink;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.spark.sql.SparkSession;

public class SinkFactory {
    
    public static Sink<String> createSink(SinkTypes sinkType, KafkaProducer<String, String> kafkaProducer, 
                                         KafkaProducerConfig kafkaConfig, S3Config s3Config, SparkSession sparkSession) {
        switch (sinkType) {
            case KAFKA:
                if (kafkaProducer == null || kafkaConfig == null) {
                    throw new IllegalArgumentException("KafkaProducer and KafkaProducerConfig are required for Kafka sink");
                }
                return new KafkaSinkImpl(kafkaProducer, kafkaConfig);
            case S3:
                if (s3Config == null || sparkSession == null) {
                    throw new IllegalArgumentException("S3Config and SparkSession are required for S3 sink");
                }
                return new S3SinkImpl(s3Config, sparkSession);
            default:
                throw new IllegalArgumentException("Invalid sink type: " + sinkType);
        }
    }
}
