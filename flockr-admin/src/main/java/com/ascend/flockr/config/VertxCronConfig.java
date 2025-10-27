package com.ascend.flockr.config;

import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class VertxCronConfig {

  private Boolean enable;
  private VertxTimerConfig timer;
  private DelayConfig delay;
}
