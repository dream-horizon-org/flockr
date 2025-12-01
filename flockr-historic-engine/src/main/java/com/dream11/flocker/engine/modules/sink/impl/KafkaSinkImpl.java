package com.dream11.flocker.engine.modules.sink.impl;

import com.dream11.flocker.engine.config.KafkaProducerConfig;
import com.dream11.flocker.engine.modules.sink.Sink;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;


@Slf4j
public class KafkaSinkImpl implements Sink<String> {
    
    private final KafkaProducer<String, String> kafkaProducer;
    private final KafkaProducerConfig kafkaConfig;

    public KafkaSinkImpl(KafkaProducer<String, String> kafkaProducer, KafkaProducerConfig kafkaConfig) {
        if (kafkaProducer == null) {
            throw new IllegalArgumentException("KafkaProducer cannot be null");
        }
        if (kafkaConfig == null) {
            throw new IllegalArgumentException("KafkaProducerConfig cannot be null");
        }
        if (kafkaConfig.getTopic() == null || kafkaConfig.getTopic().isEmpty()) {
            throw new IllegalArgumentException("Kafka topic cannot be null or empty");
        }
        this.kafkaProducer = kafkaProducer;
        this.kafkaConfig = kafkaConfig;
        log.info("KafkaSinkImpl initialized for topic: {}", kafkaConfig.getTopic());
    }

    @Override
    public void write(String data) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        
        log.debug("Sending data to Kafka topic: {}", kafkaConfig.getTopic());
        
        ProducerRecord<String, String> record = new ProducerRecord<>(kafkaConfig.getTopic(), data);
        kafkaProducer.send(record, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if (exception != null) {
                    log.error("Failed to send message to Kafka topic: {}", kafkaConfig.getTopic(), exception);
                    // Note: We can't throw here as this is async, but we log the error
                } else {
                    log.debug("Message sent successfully to Kafka topic: {}, partition: {}, offset: {}", 
                        kafkaConfig.getTopic(), metadata.partition(), metadata.offset());
                }
            }
        });
    }


    @Override
    public void flush() throws Exception {
        log.debug("Flushing Kafka producer for topic: {}", kafkaConfig.getTopic());
        kafkaProducer.flush();
        log.debug("Kafka producer flushed successfully");
    }


}
