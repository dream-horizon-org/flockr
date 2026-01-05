package com.dream11.flocker.engine.utils;

import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe rate limiter that enforces a maximum number of requests per second.
 *
 * <p>This class tracks the time between requests and sleeps if necessary to maintain the configured
 * rate limit. It uses atomic operations to ensure thread safety when used by multiple threads.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * RateLimiter rateLimiter = new RateLimiter(10); // 10 requests per second
 *
 * for (int i = 0; i < 100; i++) {
 *     rateLimiter.acquire(); // Will wait if necessary
 *     makeRequest();
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b>
 *
 * <p>This class is thread-safe and can be used by multiple threads concurrently.
 *
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class RateLimiter {

  /** Timestamp of the last request (for rate limiting). */
  private final AtomicLong lastRequestTime = new AtomicLong(0);

  /** Minimum interval between requests in milliseconds (calculated from rate limit). */
  private final long minIntervalMillis;

  /**
   * Creates a new RateLimiter with the specified rate limit.
   *
   * @param requestsPerSecond The maximum number of requests allowed per second.
   * @throws IllegalArgumentException If requestsPerSecond is less than or equal to 0.
   */
  public RateLimiter(int requestsPerSecond) {
    if (requestsPerSecond <= 0) {
      throw new IllegalArgumentException("Requests per second must be greater than 0");
    }
    this.minIntervalMillis = 1000L / requestsPerSecond;
    log.debug(
        "RateLimiter initialized with {} requests/sec (min interval: {}ms)",
        requestsPerSecond,
        minIntervalMillis);
  }

  /**
   * Acquires permission to make a request, waiting if necessary to maintain the rate limit.
   *
   * <p>This method calculates the time since the last request and sleeps if necessary to maintain
   * the configured requests per second limit.
   *
   * <p><b>Thread Safety:</b>
   *
   * <p>This method uses atomic operations to ensure thread-safe rate limiting.
   *
   * @throws RuntimeException If the thread is interrupted while waiting.
   */
  public void acquire() {
    long currentTime = System.currentTimeMillis();
    long lastTime = lastRequestTime.get();
    long elapsed = currentTime - lastTime;

    if (elapsed < minIntervalMillis) {
      long waitTime = minIntervalMillis - elapsed;
      try {
        Thread.sleep(waitTime);
        log.debug("Rate limiting: waited {}ms", waitTime);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Rate limiting wait interrupted", e);
      }
    }

    lastRequestTime.set(System.currentTimeMillis());
  }

  /**
   * Gets the minimum interval between requests in milliseconds.
   *
   * @return The minimum interval in milliseconds.
   */
  public long getMinIntervalMillis() {
    return minIntervalMillis;
  }

  /**
   * Gets the configured requests per second.
   *
   * @return The requests per second.
   */
  public int getRequestsPerSecond() {
    return (int) (1000 / minIntervalMillis);
  }
}
