package com.ascend.flockr.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class Mark7Config {

  @NotNull private String host;

  @NotNull private Integer port;

  @NotNull private Interval timeout;

  @JsonProperty("jobname")
  @NotNull
  private String jobName;
}
