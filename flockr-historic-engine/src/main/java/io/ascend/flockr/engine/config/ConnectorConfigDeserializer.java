package io.ascend.flockr.engine.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.ascend.flockr.engine.enums.SinkTypes;
import io.ascend.flockr.engine.enums.SourceTypes;
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

    String originalType = node.get("type").asText().toUpperCase();
    // Normalize WEBHOOK to API - they are treated identically
    // This MUST happen before any validation to ensure consistent type handling
    String type = "WEBHOOK".equals(originalType) ? "API" : originalType;

    // Validate the normalized type first, even if config is null
    // Always use the normalized 'type' variable, never 'originalType' for validation
    try {
      SourceTypes.valueOf(type);
    } catch (IllegalArgumentException e) {
      // Not a source type, validate as sink type using normalized type
      try {
        SinkTypes.valueOf(type); // Use normalized type, not originalType
      } catch (IllegalArgumentException ex) {
        log.error("Unknown connector type: {} (normalized from: {})", type, originalType);
        throw new IllegalArgumentException(
            "Unknown connector type: " + originalType + " (normalized to: " + type + ")", ex);
      }
    }

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
        switch (sourceType) {
          case ATHENA:
            parsedConfig = AthenaConfig.fromConfig(config);
            break;
          default:
            throw new IllegalArgumentException("Unknown source type: " + sourceType);
        }
      } catch (IllegalArgumentException e) {
        try {
          SinkTypes.valueOf(type); // Validate using normalized type
          if ("API".equals(type)) {
            Config apiConfig = config;
            if (config.hasPath("timeoutMs") && !config.hasPath("timeoutSeconds")) {
              int timeoutMs = config.getInt("timeoutMs");
              int timeoutSeconds = timeoutMs / 1000;
              apiConfig =
                  config.withValue("timeoutSeconds", ConfigValueFactory.fromAnyRef(timeoutSeconds));
            }
            parsedConfig = ApiConfig.fromConfig(apiConfig);
          } else if ("S3".equals(type)) {
            parsedConfig = S3Config.fromConfig(config);
          } else if ("KAFKA".equals(type)) {
            parsedConfig = KafkaConfig.fromConfig(config);
          } else {
            parsedConfig = config;
          }
        } catch (IllegalArgumentException ex) {
          log.error(
              "Unknown connector type after normalization: {} (original: {})", type, originalType);
          throw new IllegalArgumentException(
              "Unknown connector type: " + originalType + " (normalized to: " + type + ")", ex);
        }
      }
    }

    return new ConnectorConfig(type, parsedConfig);
  }
}
