package io.ascend.flockr.engine.config;

import com.typesafe.config.Config;
import io.ascend.flockr.engine.config.provider.ConfigProvider;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@NoArgsConstructor
public class KafkaConfig {

  private String topic;
  private String bootstrapServersUrl;
  private String connectorType;
  private Map<String, String> options;

  public static ConfigProvider<KafkaConfig> providerForSource() {
    return ConfigProvider.forSource("kafka", KafkaConfig.class);
  }

  public static ConfigProvider<KafkaConfig> providerForSink() {
    return ConfigProvider.forSink("kafka", KafkaConfig.class);
  }

  public static KafkaConfig fromConfig(Config config) {
    KafkaConfig kafkaConfig = new KafkaConfig();

    kafkaConfig.setTopic(getStringOrNull(config, "topic"));
    // Support both bootstrapServers and bootstrapServersUrl for compatibility
    String bootstrapServers = getStringOrNull(config, "bootstrapServers");
    if (bootstrapServers == null) {
      bootstrapServers = getStringOrNull(config, "bootstrapServersUrl");
    }
    kafkaConfig.setBootstrapServersUrl(bootstrapServers);
    kafkaConfig.setConnectorType(getStringOrNull(config, "connectorType"));

    if (config.hasPath("options")) {
      Map<String, String> options = new HashMap<>();
      Config optionsConfig = config.getConfig("options");
      optionsConfig.root().keySet().forEach(key -> options.put(key, optionsConfig.getString(key)));
      kafkaConfig.setOptions(options);
    }

    return kafkaConfig;
  }

  private static String getStringOrNull(Config config, String path) {
    return config.hasPath(path) ? config.getString(path) : null;
  }
}
