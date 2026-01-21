package io.ascend.flockr.engine.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngineArguments {

  @JsonProperty("audienceName")
  private String audienceName;

  @JsonProperty("action")
  private String action;

  @JsonProperty("xProjectId")
  private String xProjectId;

  /** Single source configuration. Only one source is supported. */
  @JsonProperty("source")
  private SourceConfig source;

  /** List of destination configurations. Multiple destinations are supported. */
  @JsonProperty("destinationJson")
  private List<ConnectorConfig> destinationJson;

  @JsonProperty("expireAt")
  private Long expireAt;
}
