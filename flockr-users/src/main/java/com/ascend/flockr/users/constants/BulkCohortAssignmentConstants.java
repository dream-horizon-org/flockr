package com.ascend.flockr.users.constants;

import java.time.Duration;

/**
 * Constants for bulk cohort assignment operations.
 *
 * <p>Contains configuration values for batch processing, concurrency limits, retry policies, and
 * validation patterns.
 *
 * @since 1.0
 */
public class BulkCohortAssignmentConstants {

  /** Number of user IDs to process in each batch. */
  public static final int BATCH_SIZE = 1000;

  /** Maximum number of concurrent Aerospike operations (minimum 2, or number of CPU cores). */
  public static final int MAX_CONCURRENCY = Math.max(2, Runtime.getRuntime().availableProcessors());

  /** Maximum number of retries for failed operations. */
  public static final int MAX_RETRIES = 3;

  /** Retry backoff delay duration. */
  public static final Duration RETRY_BACKOFF = Duration.ofMillis(500);

  /** Maximum file size in bytes (10 MB). */
  public static final long MAX_SIZE = 10 * 1024L * 1024L;
}
