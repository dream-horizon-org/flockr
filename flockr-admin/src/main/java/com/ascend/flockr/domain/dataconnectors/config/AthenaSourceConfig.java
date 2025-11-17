package com.ascend.flockr.domain.dataconnectors.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO for Athena data source configuration.
 * Used to parse JsonObject config retrieved from database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AthenaSourceConfig implements SourceConfig {

  /** Required: SQL query string for Athena. */
  @JsonProperty("query")
  private String query;

  /** Optional: Database name. */
  @JsonProperty("database")
  private String database;

  /** Optional: AWS region (e.g., us-east-1). */
  @JsonProperty("region")
  private String region;

  /** Optional: AWS access key ID. Must be provided together with secretKey. */
  @JsonProperty("accessKey")
  private String accessKey;

  /** Optional: AWS secret access key. Must be provided together with accessKey. */
  @JsonProperty("secretKey")
  private String secretKey;

  /** Optional: Catalog name (defaults to "AwsDataCatalog" if not specified). */
  @JsonProperty("catalog")
  private String catalog;

  /** Connector type identifier for JsonSubTypes deserialization. */
  @JsonProperty("connectorType")
  @Builder.Default
  private String connectorType = "ATHENA";

  @Override
  public String getConnectorType() {
    return connectorType;
  }
}

