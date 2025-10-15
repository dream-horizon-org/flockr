package com.ascend.flockr.common.config;

import lombok.Data;

@Data
public class AerospikeConfig {
  private String host;
  private Integer eventLoopSize;
  private Integer maxCommandsInProcess;
  private Integer maxConnectionsPerNode;
  private Integer maxCommandsInQueue;

  private String namespace;
  private String persistentCohortsSet;
  private String cohortCreatedAtBin;
  private String cohortUpdatedAtBin;
  private String cohortExpiryBin;
}
