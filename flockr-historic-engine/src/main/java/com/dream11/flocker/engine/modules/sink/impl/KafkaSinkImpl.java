package com.dream11.flocker.engine.modules.sink.impl;

import com.dream11.flocker.engine.config.KafkaProducerConfig;
import com.dream11.flocker.engine.modules.sink.Sink;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.List;
import java.util.concurrent.Future;


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
    public void writeDataset(Dataset<Row> dataset) throws Exception {
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset cannot be null");
        }

        log.info("Writing dataset to Kafka topic: {}", kafkaConfig.getTopic());

        // Convert dataset rows to JSON strings and send to Kafka
        List<Row> rows = dataset.collectAsList();
        log.info("Collected {} rows to send to Kafka", rows.size());

        int successCount = 0;
        int errorCount = 0;
        List<Future<RecordMetadata>> futures = new java.util.ArrayList<>();

        for (Row row : rows) {
            try {
                // Convert row to JSON string
                String json = rowToJson(row);
                ProducerRecord<String, String> record = new ProducerRecord<>(kafkaConfig.getTopic(), json);
                Future<RecordMetadata> future = kafkaProducer.send(record, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception exception) {
                        if (exception != null) {
                            log.error("Failed to send message to Kafka topic: {}", kafkaConfig.getTopic(), exception);
                        } else {
                            log.debug("Message sent successfully to Kafka topic: {}, partition: {}, offset: {}", 
                                kafkaConfig.getTopic(), metadata.partition(), metadata.offset());
                        }
                    }
                });
                futures.add(future);
                successCount++;
            } catch (Exception e) {
                log.error("Error converting row to JSON or sending to Kafka", e);
                errorCount++;
            }
        }

        // Wait for all sends to complete
        for (Future<RecordMetadata> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("Error waiting for Kafka send to complete", e);
            }
        }

        log.info("Successfully sent {} messages to Kafka topic: {}, errors: {}", 
            successCount, kafkaConfig.getTopic(), errorCount);
        
        if (errorCount > 0) {
            throw new RuntimeException("Failed to send " + errorCount + " messages to Kafka");
        }
    }

    /**
     * Converts a Spark Row to JSON string.
     * Simple implementation that creates a JSON object with column names as keys.
     */
    private String rowToJson(Row row) {
        StringBuilder json = new StringBuilder("{");
        String[] columns = row.schema().fieldNames();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(columns[i]).append("\":");
            Object value = row.get(i);
            if (value == null) {
                json.append("null");
            } else if (value instanceof String) {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }
        json.append("}");
        return json.toString();
    }

    /**
     * Escapes special characters in JSON strings.
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    @Override
    public void flush() throws Exception {
        log.debug("Flushing Kafka producer for topic: {}", kafkaConfig.getTopic());
        kafkaProducer.flush();
        log.debug("Kafka producer flushed successfully");
    }
}
