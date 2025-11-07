package com.ascend.flockr.common.client.impl;

import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.*;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.Replica;
import com.aerospike.client.policy.WritePolicy;
import com.ascend.flockr.common.client.Aerospike;
import com.ascend.flockr.common.config.AerospikeConfig;
import com.ascend.flockr.common.constants.Constants;
import com.google.inject.Inject;
import io.d11.aerospike.client.AerospikeClient;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.impl.AsyncResultSingle;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AerospikeImpl implements Aerospike {
  private final AerospikeClient flockrAerospikeClient;
  private final AerospikeConfig aerospikeConfig;
  private static final int MAX_RETRIES = 3;

  @Inject
  public AerospikeImpl(AerospikeClient flockrAerospikeClient, AerospikeConfig aerospikeConfig) {
    this.flockrAerospikeClient = flockrAerospikeClient;
    this.aerospikeConfig = aerospikeConfig;
  }

  @Override
  public Single<Boolean> isConnected() {
    return AsyncResultSingle.toSingle(flockrAerospikeClient::isConnected);
  }

  @Override
  public Single<Map<String, Long>> getCohortExpiryBin(String id, String set) {
    String namespace = aerospikeConfig.getNamespace();
    Key key = new Key(namespace, set, Constants.USER_KEY + id);
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
                return (Map<String, Long>) cohortMapRecord.getMap(binName);
              }
            });
  }

  @Override
  public Single<Boolean> appendCohort(
      String id, String cohort, String source, Long cohortExpiry, String setName) {
    WritePolicy writePolicy = getWritePolicy();

    String namespace = aerospikeConfig.getNamespace();

    Key key = new Key(namespace, setName, Constants.USER_KEY + id);

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

  @Override
  public Single<Boolean> removeCohort(String id, String cohort, String source, String setName) {
    WritePolicy writePolicy = getWritePolicy();

    String namespace = aerospikeConfig.getNamespace();

    Key key = new Key(namespace, setName, Constants.USER_KEY + id);

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

  private Operation getMapOperationRemoveCohort(String bin, String cohort) {
    return MapOperation.removeByKey(bin, Value.get(cohort), MapReturnType.VALUE);
  }

  private Operation getMapOperationAppendCohort(
      MapPolicy mapPolicy, String bin, String cohort, Long currentTime) {
    return MapOperation.put(mapPolicy, bin, Value.get(cohort), Value.get(currentTime));
  }

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

  private Policy getPolicy() {
    Policy policy = new Policy();
    policy.replica = Replica.MASTER_PROLES;
    policy.sendKey = true;
    return policy;
  }

  private WritePolicy getWritePolicy() {
    WritePolicy writePolicy = new WritePolicy();
    writePolicy.maxRetries = MAX_RETRIES;
    writePolicy.sendKey = true;
    return writePolicy;
  }
}
