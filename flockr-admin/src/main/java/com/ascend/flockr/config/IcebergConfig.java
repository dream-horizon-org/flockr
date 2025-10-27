package com.ascend.flockr.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class IcebergConfig {
  @JsonProperty("realtimeconfig")
  private IcebergRealTimeConfig realTimeConfig;

  @JsonProperty("historicconfig")
  private IcebergHistoricConfig historicConfig;

  @JsonProperty("format")
  private String format;

  @JsonProperty("tablename")
  private String tableName;

  @JsonProperty("mode")
  private String mode;

  @JsonProperty("mergequery")
  private String mergeQuery;

  @JsonProperty("resourcetier")
  private String resourceTier;

  @JsonProperty("retrycount")
  private Long retryCount;

  @JsonProperty("retrydelay")
  private Long retryDelay;

  @JsonProperty("processrecordthreshold")
  private Integer processRecordThreshold;

  @JsonProperty("processtimethreshold")
  private Threshold processTimeThreshold;

  @JsonProperty("ingestiblebucketsthreshold")
  private Integer ingestibleBucketsThreshold;

  @JsonProperty("countjob")
  private CountJobConfig countJobConfig;

  @Getter
  public static class IcebergRealTimeConfig {
    @JsonProperty("topicname")
    private String topicName;

    @JsonProperty("brokername")
    private String brokerName;
  }

  @Getter
  public static class IcebergHistoricConfig {
    @JsonProperty("s3Path")
    private String s3path;
  }

  @Getter
  public static class CountJobConfig {
    @JsonProperty("countjobbatchsize")
    private Integer countJobBatchSize;

    @JsonProperty("resourcetier")
    private String resourceTier;

    @JsonProperty("retrycount")
    private Long retryCount;

    @JsonProperty("retrydelay")
    private Long retryDelay;
  }
}
