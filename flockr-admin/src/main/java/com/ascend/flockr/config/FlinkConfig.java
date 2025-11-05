package com.ascend.flockr.config;

import lombok.Getter;

@Getter
public class FlinkConfig {

  private String host;
  private Integer port;
  private Interval timeout;
  private String directory;
}
