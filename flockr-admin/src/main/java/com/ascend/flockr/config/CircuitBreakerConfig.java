package com.ascend.flockr.config;

import com.ascend.flockr.config.provider.ConfigProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration class for Resilience4j Circuit Breaker settings.
 *
 * <p>This configuration is loaded from {@code config/circuit-breaker/default.conf} and contains
 * all settings needed to configure circuit breakers, including failure thresholds, timeouts, and
 * sliding window settings.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
public class CircuitBreakerConfig {
  private int failureRateThreshold;
  private int slowCallRateThreshold;
  private int waitDurationInOpenState;
  private int slowCallDurationThreshold;
  private int permittedNumberOfCallsInHalfOpenState;
  private int minimumNumberOfCalls;
  private int slidingWindowSize;

  /**
   * Creates a provider for CircuitBreakerConfig that loads configuration from the circuit-breaker
   * config directory.
   *
   * @return a ConfigProvider instance for CircuitBreakerConfig
   */
  public static ConfigProvider<CircuitBreakerConfig> provider() {
    return new ConfigProvider<>("circuit-breaker", CircuitBreakerConfig.class);
  }
}
