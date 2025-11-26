package io.ascend.flockr.admin.config;

import com.typesafe.config.Optional;
import io.ascend.flockr.admin.config.provider.ConfigProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AerospikeConfig {
  private static final Integer DEFAULT_PORT = 3000;

  private String host;
  @Optional private Integer port = DEFAULT_PORT;
  private int maxRetries;
  private int maxConnsPerNode;
  private int eventLoopSize;
  private int maxCommandsInProcess;
  private int maxCommandsInQueue;
  private long connectRetryIntervalMS;

  private String namespace;

  public static ConfigProvider<AerospikeConfig> provider() {
    return new ConfigProvider<>("aerospike", AerospikeConfig.class);
  }
}
