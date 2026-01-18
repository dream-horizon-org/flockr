package io.ascend.flockr.engine.config;

import com.typesafe.config.Config;
import io.ascend.flockr.engine.config.provider.ConfigProvider;
import java.io.Serializable;
import lombok.Data;

@Data
public class KafkaProducerConfig implements Serializable {

  private String bootstrapServers;
  private String topic;
  private String keySerializerClass;
  private String valueSerializerClass;

  public static ConfigProvider<KafkaProducerConfig> provider() {
    return ConfigProvider.forSink("kafka", KafkaProducerConfig.class);
  }

  public static KafkaProducerConfig fromConfig(Config config) {
    KafkaProducerConfig kafkaConfig = new KafkaProducerConfig();

    String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
    if (bootstrapServers == null || bootstrapServers.isEmpty()) {
      bootstrapServers = System.getProperty("kafka.bootstrap.servers");
    }
    if ((bootstrapServers == null || bootstrapServers.isEmpty())
        && config.hasPath("bootstrapServers")) {
      bootstrapServers = config.getString("bootstrapServers");
    }
    if ((bootstrapServers == null || bootstrapServers.isEmpty())
        && config.hasPath("bootstrapServersUrl")) {
      bootstrapServers = config.getString("bootstrapServersUrl");
    }
    if (bootstrapServers != null && !bootstrapServers.isEmpty()) {
      kafkaConfig.setBootstrapServers(bootstrapServers);
    }

    if (config.hasPath("topic")) {
      kafkaConfig.setTopic(config.getString("topic"));
    }
    if (config.hasPath("keySerializerClass")) {
      kafkaConfig.setKeySerializerClass(config.getString("keySerializerClass"));
    }
    if (config.hasPath("valueSerializerClass")) {
      kafkaConfig.setValueSerializerClass(config.getString("valueSerializerClass"));
    }
    return kafkaConfig;
  }
}
