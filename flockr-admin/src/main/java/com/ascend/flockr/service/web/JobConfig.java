package com.ascend.flockr.service.web;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobConfig {
  // TODO use Java Generics

  private String clientUser;
  private String requestId = UUID.randomUUID().toString();
  private Long executionId;

  public JobConfig(String clientUser) {
    this.clientUser = clientUser;
  }
}
