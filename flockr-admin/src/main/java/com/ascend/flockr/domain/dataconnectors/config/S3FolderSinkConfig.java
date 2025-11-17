package com.ascend.flockr.domain.dataconnectors.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO for S3 folder sink configuration.
 * Used to parse JsonObject config retrieved from database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class S3FolderSinkConfig implements SinkConfig {

  /** Required: S3 bucket name. */
  @JsonProperty("bucket")
  private String bucket;

  /** Required: Folder path within the bucket (e.g., "data/audiences" or "exports/2024"). */
  @JsonProperty("folderPath")
  private String folderPath;

  /** Optional: AWS region (e.g., us-east-1). */
  @JsonProperty("region")
  private String region;

  /** Optional: AWS access key ID. Must be provided together with secretKey. */
  @JsonProperty("accessKey")
  private String accessKey;

  /** Optional: AWS secret access key. Must be provided together with accessKey. */
  @JsonProperty("secretKey")
  private String secretKey;

  /** Optional: File format for output files (json, csv, parquet). */
  @JsonProperty("fileFormat")
  private String fileFormat;

  /** Connector type identifier for JsonSubTypes deserialization. */
  @JsonProperty("connectorType")
  @Builder.Default
  private String connectorType = "S3_FOLDER";

  @Override
  public String getConnectorType() {
    return connectorType;
  }
}

