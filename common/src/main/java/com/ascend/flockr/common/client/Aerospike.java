package com.ascend.flockr.common.client;

import io.reactivex.rxjava3.core.Single;
import java.util.Map;

/**
 * Client interface for interacting with Aerospike database for cohort management operations.
 *
 * <p>This interface provides reactive (RxJava) methods for:
 * <ul>
 *   <li>Checking connection status
 *   <li>Retrieving cohort expiry information
 *   <li>Adding users to cohorts (append operations)
 *   <li>Removing users from cohorts (remove operations)
 * </ul>
 *
 * <p>All methods return {@link Single} for asynchronous, non-blocking operations. The
 * implementation handles connection management, retries, and error handling internally.
 *
 * <p><strong>Key Concepts:</strong>
 *
 * <ul>
 *   <li><strong>Set Name:</strong> Aerospike set (similar to a table) where user records are
 *       stored
 *   <li><strong>Cohort:</strong> A named group that users can belong to
 *   <li><strong>Source:</strong> The origin/platform that created the cohort assignment (e.g.,
 *       "Dream11", "FanCode")
 *   <li><strong>Cohort Expiry:</strong> Timestamp (epoch milliseconds) when the cohort
 *       membership expires
 * </ul>
 *
 * <p><strong>Implementation:</strong>
 *
 * <p>The default implementation is {@link com.ascend.flockr.common.client.impl.AerospikeImpl},
 * which uses the Aerospike Java client library.
 *
 * @author Flockr Team
 * @since 1.0
 * @see com.ascend.flockr.common.client.impl.AerospikeImpl
 */
public interface Aerospike {
  /**
   * Checks if the Aerospike client is connected to the cluster.
   *
   * <p>This method performs a lightweight connectivity check. It does not guarantee that all
   * cluster nodes are reachable, only that the client has an active connection.
   *
   * @return a {@link Single} that emits {@code true} if connected, {@code false} otherwise
   */
  Single<Boolean> isConnected();

  /**
   * Retrieves the cohort expiry bin for a given user ID.
   *
   * <p>This method fetches the map containing cohort names as keys and their expiry timestamps
   * (epoch milliseconds) as values. If the user does not exist or has no cohorts, an empty map is
   * returned.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Single<Map<String, Long>> expiryMap = aerospike.getCohortExpiryBin("12345", "users");
   * // Returns: {"premium-users": 1704067200000L, "vip-users": 1704153600000L}
   * }</pre>
   *
   * @param id the user ID or guest ID (without the "user_" prefix)
   * @param setName the Aerospike set name where the user record is stored
   * @return a {@link Single} emitting a map of cohort names to expiry timestamps (epoch
   *     milliseconds), or an empty map if the user has no cohorts
   */
  Single<Map<String, Long>> getCohortExpiryBin(String id, String setName);

  /**
   * Adds a user to a cohort with the specified expiry time.
   *
   * <p>This method performs an append operation that:
   * <ul>
   *   <li>Adds or updates the cohort in the expiry bin with the provided expiry timestamp
   *   <li>Updates the cohort's "updatedAt" timestamp to the current time
   *   <li>Sets the cohort's "createdAt" timestamp only if it doesn't already exist
   * </ul>
   *
   * <p><strong>Operation Details:</strong>
   *
   * <p>The operation uses Aerospike map operations to atomically update multiple bins:
   * <ul>
   *   <li>{@code cohortExpiryBin}: Maps cohort name to expiry timestamp
   *   <li>{@code cohortUpdatedAtBin}: Maps cohort name to last update timestamp
   *   <li>{@code cohortCreatedAtBin}: Maps cohort name to creation timestamp (create-only)
   * </ul>
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Single<Boolean> result = aerospike.appendCohort(
   *     "12345",                    // user ID
   *     "premium-users",            // cohort name
   *     "Dream11",                  // source
   *     1704067200000L,             // expiry timestamp
   *     "users"                     // set name
   * );
   * }</pre>
   *
   * @param id the user ID or guest ID (without the "user_" prefix)
   * @param cohort the name of the cohort to add the user to
   * @param source the source/platform identifier (e.g., "Dream11", "FanCode")
   * @param cohortExpiry the expiry timestamp in epoch milliseconds (UTC)
   * @param setName the Aerospike set name where the user record is stored
   * @return a {@link Single} that emits {@code true} when the operation completes successfully
   */
  Single<Boolean> appendCohort(
      String id, String cohort, String source, Long cohortExpiry, String setName);

  /**
   * Removes a user from a cohort.
   *
   * <p>This method performs a remove operation that deletes the cohort from all relevant bins:
   * <ul>
   *   <li>Removes the cohort from the expiry bin
   *   <li>Removes the cohort from the "createdAt" bin
   *   <li>Removes the cohort from the "updatedAt" bin
   * </ul>
   *
   * <p><strong>Operation Details:</strong>
   *
   * <p>The operation uses Aerospike map operations to atomically remove the cohort from multiple
   * bins. If the cohort doesn't exist, the operation still succeeds (idempotent).
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * Single<Boolean> result = aerospike.removeCohort(
   *     "12345",           // user ID
   *     "premium-users",  // cohort name
   *     "Dream11",        // source
   *     "users"           // set name
   * );
   * }</pre>
   *
   * @param id the user ID or guest ID (without the "user_" prefix)
   * @param cohort the name of the cohort to remove the user from
   * @param source the source/platform identifier (e.g., "Dream11", "FanCode")
   * @param setName the Aerospike set name where the user record is stored
   * @return a {@link Single} that emits {@code true} when the operation completes successfully
   */
  Single<Boolean> removeCohort(String id, String cohort, String source, String setName);
}
