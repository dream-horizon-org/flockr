package com.ascend.flockr.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class RuleConfig {

  private RealTime realtime;
  private Historic historic;
  private Long expiryTaskStartTimeDelayMillis;
  private Long expiryTaskEndTimeDelayMillis;
  private String systemAlertsSlackChannel;

  @Getter
  public static class RealTime {
    @JsonProperty("flash")
    private FlashConfig flashConfig;

    @JsonProperty("sequence")
    private PatternSequenceConfig sequenceConfig;

    private Long watermark;
    private SourceConfig source;
    private Filter filter;

    @JsonProperty("slackchannel")
    private String slackChannel;

    @JsonProperty("iceberg")
    private IcebergConfig iceberg;
  }

  @Getter
  public static class Historic {
    @JsonProperty("slackchannel")
    private String slackChannel;

    @JsonProperty("resourcetier")
    private String resourceTier;

    @JsonProperty("retrycount")
    private Long retryCount;

    @JsonProperty("redshiftenginename")
    private String redshiftEngineName;

    @JsonProperty("iceberg")
    private IcebergConfig iceberg;
  }

  @Getter
  public static class SourceConfig {
    private String broker;
  }

  @Getter
  public static class Filter {
    @JsonProperty("filterbeforepatterncomputation")
    private boolean filterBeforePatternComputation;
  }

  @Getter
  public static class FlashConfig {
    private Integer parallelism;
  }

  @Getter
  public static class PatternSequenceConfig {
    private Integer parallelism;
  }
}
