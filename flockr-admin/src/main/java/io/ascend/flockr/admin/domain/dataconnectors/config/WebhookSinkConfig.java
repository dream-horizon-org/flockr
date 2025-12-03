package io.ascend.flockr.admin.domain.dataconnectors.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO for Webhook/API sink connector configuration. Used to call external HTTP endpoints with
 * audience data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookSinkConfig implements SinkConfig {

  /** Required: The webhook/API URL to call. */
  @JsonProperty("url")
  private String url;

  /** Optional: HTTP method to use (POST or PUT). Defaults to POST. */
  @JsonProperty("method")
  @Builder.Default
  private String method = "POST";

  /** Optional: Custom headers to include in the request (e.g., Authorization, API keys). */
  @JsonProperty("headers")
  private Map<String, String> headers;

  /** Optional: Request timeout in milliseconds. Defaults to 30000 (30 seconds). */
  @JsonProperty("timeoutMs")
  @Builder.Default
  private Integer timeoutMs = 30000;

  /** Optional: Content type for the request body. Defaults to application/json. */
  @JsonProperty("contentType")
  @Builder.Default
  private String contentType = "application/json";

  /** Optional: Whether to send records individually or as a batch array. Defaults to batch. */
  @JsonProperty("batchMode")
  @Builder.Default
  private Boolean batchMode = true;

  /** Connector type identifier for JsonSubTypes deserialization. */
  @JsonProperty("connectorType")
  @Builder.Default
  private String connectorType = "WEBHOOK";

  @Override
  public String getConnectorType() {
    return connectorType;
  }
}
