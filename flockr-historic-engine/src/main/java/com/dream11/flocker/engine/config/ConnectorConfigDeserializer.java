package com.dream11.flocker.engine.config;

import com.dream11.flocker.engine.enums.SinkTypes;
import com.dream11.flocker.engine.enums.SourceTypes;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectorConfigDeserializer extends JsonDeserializer<ConnectorConfig> {

  @Override
  public ConnectorConfig deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);

    if (!node.has("type")) {
      throw new IllegalArgumentException("Connector configuration must have 'type' field");
    }

    String type = node.get("type").asText().toUpperCase();
    JsonNode configNode = node.has("config") ? node.get("config") : null;

    Object parsedConfig = null;

    if (configNode != null) {
      ObjectMapper mapper = new ObjectMapper();
      @SuppressWarnings("unchecked")
      Map<String, Object> configMap =
          (Map<String, Object>) mapper.convertValue(configNode, Map.class);
      Config config = ConfigFactory.parseMap(configMap);

      try {
        SourceTypes sourceType = SourceTypes.valueOf(type);
        parsedConfig =
            switch (sourceType) {
              case S3 -> S3Config.fromConfig(config);
              case ATHENA -> AthenaConfig.fromConfig(config);
              case KAFKA -> KafkaConfig.fromConfig(config);
              case REDSHIFT -> throw new UnsupportedOperationException(
                  "Redshift source not yet implemented");
            };
      } catch (IllegalArgumentException e) {
        try {
          SinkTypes.valueOf(type); // Validate it's a valid sink type
          if ("API".equals(type)) {
            parsedConfig = ApiConfig.fromConfig(config);
          } else if ("S3".equals(type)) {
            parsedConfig = S3Config.fromConfig(config);
          } else if ("KAFKA".equals(type)) {
            parsedConfig = KafkaConfig.fromConfig(config);
          } else {
            parsedConfig = config;
          }
        } catch (IllegalArgumentException ex) {
          log.error("Unknown connector type: {}", type);
          throw new IllegalArgumentException("Unknown connector type: " + type, ex);
        }
      }
    }

    return new ConnectorConfig(type, parsedConfig);
  }
}
