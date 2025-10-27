package com.ascend.flockr.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class SlackConfig {
  private String host;
  private Interval timeout;
  private Bot nucleus;
  private Channel cdp;

  @Getter
  public static class Bot {
    private String tokenAEBot;
    private String tokenNonAEBot;
  }

  @Getter
  public static class Channel {
    @JsonProperty("channelid")
    private String channelId;
  }
}
