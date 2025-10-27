package com.ascend.flockr.config;

import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class DataTitanConfig {
  private String host;
  private Integer port;
  private Interval timeout;
  private JwtConfig jwt;
  private ConsumerConfig consumer;

  @Getter
  public static class JwtConfig {
    private String audience;
  }
}
