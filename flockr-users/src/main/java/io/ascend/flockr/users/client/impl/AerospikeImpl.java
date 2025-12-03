package io.ascend.flockr.users.client.impl;

import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.*;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.Replica;
import com.aerospike.client.policy.WritePolicy;
import com.google.inject.Inject;
import io.ascend.flockr.users.client.Aerospike;
import io.ascend.flockr.users.config.AerospikeConfig;
import io.d11.aerospike.client.AerospikeClient;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.impl.AsyncResultSingle;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Aerospike} interface using the Aerospike Java client.
 *
 * <p>This implementation provides reactive (RxJava) operations for cohort management in Aerospike.
 * It uses Aerospike's map data type (CDT) to store cohort information in multiple bins:
 *
 * <ul>
 *   <li><strong>Cohort Expiry Bin:</strong> Maps cohort names to expiry timestamps
 *   <li><strong>Cohort Created At Bin:</strong> Maps cohort names to creation timestamps
 *   <li><strong>Cohort Updated At Bin:</strong> Maps cohort names to last update timestamps
 * </ul>
 *
 * <p><strong>Key Features:</strong>
 *
 * <ul>
 *   <li>Atomic operations using Aerospike map operations
 *   <li>Automatic retry on write failures (up to {@value #MAX_RETRIES} retries)
 *   <li>Replica selection policy for read operations (MASTER_PROLES)
 *   <li>Error logging for failed operations
 * </ul>
 *
 * <p><strong>Thread Safety:</strong>
 *
 * <p>This implementation is thread-safe. The underlying Aerospike client handles concurrent
 * operations safely.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 * @see Aerospike
 */
@Slf4j
public class AerospikeImpl implements Aerospike {
  private final AerospikeClient flockrAerospikeClient;
  private final AerospikeConfig aerospikeConfig;

  /** Maximum number of retries for write operations. */
  private static final int MAX_RETRIES = 3;

  /**
   * Constructs a new AerospikeImpl instance.
   *
   * @param flockrAerospikeClient the Aerospike client instance
   * @param aerospikeConfig the Aerospike configuration containing namespace, set names, and bin
   *     names
   */
  @Inject
  public AerospikeImpl(AerospikeClient flockrAerospikeClient, AerospikeConfig aerospikeConfig) {
    this.flockrAerospikeClient = flockrAerospikeClient;
    this.aerospikeConfig = aerospikeConfig;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation delegates to the underlying Aerospike client's connection check.
   */
  @Override
  public Single<Boolean> isConnected() {
    return AsyncResultSingle.toSingle(flockrAerospikeClient::isConnected);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation constructs an Aerospike key using the namespace from config, the
   * provided set name, and the user ID directly as a string. It reads from the cohort expiry bin
   * configured in {@link AerospikeConfig#getCohortExpiryBin()}.
   *
   * <p>If the record doesn't exist or the bin is empty, an empty map is returned.
   */
  @Override
  public Single<Map<String, Long>> getCohortExpiryBin(String id, String set) {
    String namespace = aerospikeConfig.getNamespace();
    Key key = new Key(namespace, set, id);
    Policy policy = getPolicy();

    String binName = aerospikeConfig.getCohortExpiryBin();
    return AsyncResultSingle.<com.aerospike.client.Record>toSingle(
            asyncResultHandler ->
                flockrAerospikeClient.get(policy, key, new String[] {binName}, asyncResultHandler))
        .map(
            cohortMapRecord -> {
              if (cohortMapRecord == null
                  || cohortMapRecord.bins == null
                  || !cohortMapRecord.bins.containsKey(binName)) {
                log.warn("No record found for userId: {} in bin: {}", id, binName);
                return Map.of();
              } else {
                // Aerospike returns Map<?, ?> but we know it's Map<String, Long> from our schema
                @SuppressWarnings("unchecked")
                Map<String, Long> cohortMap = (Map<String, Long>) cohortMapRecord.getMap(binName);
                return cohortMap;
              }
            });
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation performs atomic map operations to update three bins:
   *
   * <ul>
   *   <li>Expiry bin: Updates the cohort expiry timestamp
   *   <li>UpdatedAt bin: Sets the current timestamp (always updates)
   *   <li>CreatedAt bin: Sets the current timestamp only if the cohort doesn't exist
   * </ul>
   *
   * <p>The operation uses {@link MapWriteMode#UPDATE} for expiry/updatedAt bins and {@link
   * MapWriteFlags#CREATE_ONLY} for the createdAt bin to prevent overwriting existing creation
   * timestamps.
   */
  @Override
  public Single<Boolean> appendCohort(
      String id, String cohort, String source, Long cohortExpiry, String setName) {
    WritePolicy writePolicy = getWritePolicy();

    String namespace = aerospikeConfig.getNamespace();

    Key key = new Key(namespace, setName, id);

    Operation[] operations = getMapOperations(cohort, cohortExpiry);

    return AsyncResultSingle.<com.aerospike.client.Record>toSingle(
            handler -> flockrAerospikeClient.operate(writePolicy, key, operations, handler))
        .map(appendRecord -> true)
        .doOnError(
            err -> {
              //              DatadogUtils.sendErrorToDatadog(err);
              log.error("Failed to append cohort: {} for userId: {} due error : ", cohort, id, err);
            });
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation performs atomic map operations to remove the cohort from three bins:
   *
   * <ul>
   *   <li>Expiry bin: Removes the cohort entry
   *   <li>CreatedAt bin: Removes the cohort entry
   *   <li>UpdatedAt bin: Removes the cohort entry
   * </ul>
   *
   * <p>The operation is idempotent - if the cohort doesn't exist, it still succeeds.
   */
  @Override
  public Single<Boolean> removeCohort(String id, String cohort, String source, String setName) {
    WritePolicy writePolicy = getWritePolicy();

    String namespace = aerospikeConfig.getNamespace();

    Key key = new Key(namespace, setName, id);

    Operation[] operations =
        new Operation[] {
          getMapOperationRemoveCohort(aerospikeConfig.getCohortExpiryBin(), cohort),
          getMapOperationRemoveCohort(aerospikeConfig.getCohortCreatedAtBin(), cohort),
          getMapOperationRemoveCohort(aerospikeConfig.getCohortUpdatedAtBin(), cohort)
        };

    return AsyncResultSingle.<Record>toSingle(
            handler -> flockrAerospikeClient.operate(writePolicy, key, operations, handler))
        .map(removeRecord -> true)
        .doOnError(
            err -> {
              //              DatadogUtils.sendErrorToDatadog(err);
              log.error("Failed to remove cohort: {} for userId: {} due error: ", cohort, id, err);
            });
  }

  /**
   * Creates a map operation to remove a cohort from a bin.
   *
   * @param bin the bin name to remove from
   * @param cohort the cohort name to remove
   * @return a MapOperation that removes the cohort entry
   */
  private Operation getMapOperationRemoveCohort(String bin, String cohort) {
    return MapOperation.removeByKey(bin, Value.get(cohort), MapReturnType.VALUE);
  }

  /**
   * Creates a map operation to append/update a cohort in a bin.
   *
   * @param mapPolicy the map policy (determines update/create behavior)
   * @param bin the bin name to update
   * @param cohort the cohort name
   * @param currentTime the timestamp value to store
   * @return a MapOperation that puts the cohort entry
   */
  private Operation getMapOperationAppendCohort(
      MapPolicy mapPolicy, String bin, String cohort, Long currentTime) {
    return MapOperation.put(mapPolicy, bin, Value.get(cohort), Value.get(currentTime));
  }

  /**
   * Creates an array of map operations for appending a cohort.
   *
   * <p>Creates three operations:
   *
   * <ul>
   *   <li>Update expiry bin with cohortExpiry timestamp
   *   <li>Update updatedAt bin with current timestamp
   *   <li>Create createdAt bin with current timestamp (only if doesn't exist)
   * </ul>
   *
   * @param cohort the cohort name
   * @param cohortExpiry the expiry timestamp for the cohort
   * @return an array of MapOperations to execute atomically
   */
  private Operation[] getMapOperations(String cohort, Long cohortExpiry) {
    Long currentTime = System.currentTimeMillis();

    MapPolicy unorderedUpdatePolicy = new MapPolicy(MapOrder.UNORDERED, MapWriteMode.UPDATE);
    MapPolicy unorderedCreateOnlyPolicy =
        new MapPolicy(MapOrder.UNORDERED, MapWriteFlags.NO_FAIL | MapWriteFlags.CREATE_ONLY);

    return new Operation[] {
      getMapOperationAppendCohort(
          unorderedUpdatePolicy, aerospikeConfig.getCohortExpiryBin(), cohort, cohortExpiry),
      getMapOperationAppendCohort(
          unorderedUpdatePolicy, aerospikeConfig.getCohortUpdatedAtBin(), cohort, currentTime),
      getMapOperationAppendCohort(
          unorderedCreateOnlyPolicy, aerospikeConfig.getCohortCreatedAtBin(), cohort, currentTime)
    };
  }

  /**
   * Creates a read policy for Aerospike operations.
   *
   * <p>Configures the policy to:
   *
   * <ul>
   *   <li>Read from master and prole replicas (MASTER_PROLES)
   *   <li>Send the key with the request (for debugging/monitoring)
   * </ul>
   *
   * @return a configured Policy instance for read operations
   */
  private Policy getPolicy() {
    Policy policy = new Policy();
    policy.replica = Replica.MASTER_PROLES;
    policy.sendKey = true;
    return policy;
  }

  /**
   * Creates a write policy for Aerospike operations.
   *
   * <p>Configures the policy to:
   *
   * <ul>
   *   <li>Retry up to {@value #MAX_RETRIES} times on failure
   *   <li>Send the key with the request (for debugging/monitoring)
   * </ul>
   *
   * @return a configured WritePolicy instance for write operations
   */
  private WritePolicy getWritePolicy() {
    WritePolicy writePolicy = new WritePolicy();
    writePolicy.maxRetries = MAX_RETRIES;
    writePolicy.sendKey = true;
    return writePolicy;
  }

  public Single<Boolean> appendBatchCohort(
      String id, String cohort, String source, Long cohortExpiry, String setName) {
    WritePolicy writePolicy = getWritePolicy();

    String namespace = aerospikeConfig.getNamespace();

    Key key = new Key(namespace, setName, id);

    Operation[] operations = getMapOperations(cohort, cohortExpiry);

    return AsyncResultSingle.<com.aerospike.client.Record>toSingle(
            handler -> flockrAerospikeClient.operate(writePolicy, key, operations, handler))
        .map(appendRecord -> true)
        .doOnError(
            err -> {
              log.error("Failed to append cohort: {} for userId: {} due error : ", cohort, id, err);
            });
  }

  //  private BatchWritePolicy getBatchWritePolicy() {
  //    BatchWritePolicy writePolicy = new BatchWritePolicy();
  //    writePolicy.maxRetries = MAX_RETRIES;
  //    writePolicy.sendKey = true;
  //    return writePolicy;
  //  }
}
