package com.ascend.flockr.config;

import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class FlinkConfig {

  private String host;
  private Integer port;
  private Interval timeout;
  private String directory;
}
