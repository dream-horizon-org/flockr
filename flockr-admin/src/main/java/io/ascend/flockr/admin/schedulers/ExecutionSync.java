package io.ascend.flockr.admin.schedulers;

import io.reactivex.rxjava3.core.Maybe;
import java.time.Duration;

/**
 * Interface for distributed synchronization of scheduled task execution.
 *
 * <p>Provides lease-based locking with TTL (Time-To-Live) to ensure only one instance executes at a
 * time across multiple application instances. The TTL also acts as a cooldown timer, enforcing
 * minimum intervals between executions.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public interface ExecutionSync {

  /**
   * Attempts to acquire a distributed lease for the given key.
   *
   * <p>If the lease is available (not held or expired), it will be acquired and the Maybe completes
   * with a signal. If already held by another instance, returns empty Maybe.
   *
   * @param key the unique identifier for the lease
   * @param ttl the duration after which the lease automatically expires
   * @return a Maybe that emits if acquired, or empty if the lease is held by another
   */
  Maybe<Boolean> acquire(String key, Duration ttl);
}
