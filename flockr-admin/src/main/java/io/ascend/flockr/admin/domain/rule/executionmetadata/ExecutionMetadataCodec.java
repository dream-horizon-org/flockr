package io.ascend.flockr.admin.domain.rule.executionmetadata;

import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Codec for serializing/deserializing ExecutionMetadata to/from JsonObject.
 *
 * <p>Handles type discrimination based on the "type" field in JSON.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class ExecutionMetadataCodec {

  private static final String TYPE_FIELD = "type";
  private static final String TYPE_BATCH = "BATCH";
  private static final String TYPE_STREAM = "STREAM";

  /**
   * Converts typed metadata to JsonObject for database storage.
   *
   * @param metadata the execution metadata
   * @return JsonObject representation, or empty JsonObject if null
   */
  public static JsonObject toJson(ExecutionMetadata metadata) {
    if (metadata == null) {
      return new JsonObject();
    }
    return JsonObject.mapFrom(metadata);
  }

  /**
   * Deserializes JsonObject to typed metadata based on "type" field.
   *
   * @param json the JSON object from database
   * @return typed ExecutionMetadata, or null if json is null/empty
   * @throws IllegalArgumentException if type is unknown
   */
  public static ExecutionMetadata fromJson(JsonObject json) {
    if (json == null || json.isEmpty()) {
      return null;
    }

    String type = json.getString(TYPE_FIELD);
    if (type == null) {
      log.warn("Metadata JSON missing 'type' field: {}", json.encode());
      return null;
    }

    return switch (type) {
      case TYPE_BATCH -> json.mapTo(BatchExecutionMetadata.class);
      case TYPE_STREAM -> json.mapTo(StreamExecutionMetadata.class);
      default -> {
        log.error("Unknown metadata type: {}", type);
        throw new IllegalArgumentException("Unknown metadata type: " + type);
      }
    };
  }

  /**
   * Type-safe deserialization for BATCH metadata.
   *
   * @param json the JSON object
   * @return BatchExecutionMetadata
   * @throws IllegalArgumentException if json is not BATCH type
   */
  public static BatchExecutionMetadata toBatchMetadata(JsonObject json) {
    if (json == null || json.isEmpty()) {
      return BatchExecutionMetadata.builder().build();
    }

    String type = json.getString(TYPE_FIELD);
    if (type != null && !TYPE_BATCH.equals(type)) {
      throw new IllegalArgumentException("Expected BATCH metadata but got: " + type);
    }

    return json.mapTo(BatchExecutionMetadata.class);
  }

  /**
   * Type-safe deserialization for STREAM metadata.
   *
   * @param json the JSON object
   * @return StreamExecutionMetadata
   * @throws IllegalArgumentException if json is not STREAM type
   */
  public static StreamExecutionMetadata toStreamMetadata(JsonObject json) {
    if (json == null || json.isEmpty()) {
      return StreamExecutionMetadata.builder().build();
    }

    String type = json.getString(TYPE_FIELD);
    if (type != null && !TYPE_STREAM.equals(type)) {
      throw new IllegalArgumentException("Expected STREAM metadata but got: " + type);
    }

    return json.mapTo(StreamExecutionMetadata.class);
  }

  /**
   * Creates initial BATCH metadata with submit config.
   *
   * @param driverMemory driver memory config
   * @param executorMemory executor memory config
   * @param executorCores cores per executor
   * @param executorInstances number of executors
   * @return BatchExecutionMetadata with submit config populated
   */
  public static BatchExecutionMetadata createBatchMetadata(
      String driverMemory,
      String executorMemory,
      Integer executorCores,
      Integer executorInstances) {
    return BatchExecutionMetadata.builder()
        .driverMemory(driverMemory)
        .executorMemory(executorMemory)
        .executorCores(executorCores)
        .executorInstances(executorInstances)
        .attemptNumber(1)
        .build();
  }

  /**
   * Creates initial STREAM metadata with submit config.
   *
   * @param jarId Flink JAR ID
   * @param entryClass entry class name
   * @param parallelism job parallelism
   * @param savepointPath savepoint to resume from (nullable)
   * @return StreamExecutionMetadata with submit config populated
   */
  public static StreamExecutionMetadata createStreamMetadata(
      String jarId, String entryClass, Integer parallelism, String savepointPath) {
    return StreamExecutionMetadata.builder()
        .jarId(jarId)
        .entryClass(entryClass)
        .parallelism(parallelism)
        .resumedFromSavepoint(savepointPath)
        .restartCount(0)
        .build();
  }

  /**
   * Updates BATCH metadata with runtime info from Spark status response.
   *
   * @param existing existing metadata
   * @param applicationId Spark application ID
   * @param workerId worker ID
   * @param serverSparkVersion Spark version
   * @return updated metadata
   */
  public static BatchExecutionMetadata updateBatchRuntime(
      BatchExecutionMetadata existing,
      String applicationId,
      String workerId,
      String serverSparkVersion) {
    return BatchExecutionMetadata.builder()
        .type(existing.getType())
        .driverMemory(existing.getDriverMemory())
        .executorMemory(existing.getExecutorMemory())
        .executorCores(existing.getExecutorCores())
        .executorInstances(existing.getExecutorInstances())
        .applicationId(applicationId)
        .workerId(workerId)
        .serverSparkVersion(serverSparkVersion)
        .attemptNumber(existing.getAttemptNumber())
        .build();
  }

  /**
   * Updates STREAM metadata with runtime info from Flink job details.
   *
   * @param existing existing metadata
   * @param clusterId Flink cluster ID
   * @param flinkVersion Flink version
   * @param totalTasks total tasks
   * @param runningTasks running tasks
   * @param latestCheckpointPath latest checkpoint path
   * @param restartCount restart count
   * @return updated metadata
   */
  public static StreamExecutionMetadata updateStreamRuntime(
      StreamExecutionMetadata existing,
      String clusterId,
      String flinkVersion,
      Integer totalTasks,
      Integer runningTasks,
      String latestCheckpointPath,
      Integer restartCount) {
    return StreamExecutionMetadata.builder()
        .type(existing.getType())
        .jarId(existing.getJarId())
        .entryClass(existing.getEntryClass())
        .parallelism(existing.getParallelism())
        .resumedFromSavepoint(existing.getResumedFromSavepoint())
        .clusterId(clusterId)
        .flinkVersion(flinkVersion)
        .totalTasks(totalTasks)
        .runningTasks(runningTasks)
        .latestCheckpointPath(latestCheckpointPath)
        .restartCount(restartCount)
        .build();
  }
}
