package io.ascend.flockr.admin.domain.rule.executionmetadata;

/**
 * Sealed interface for execution metadata.
 *
 * <p>Implementations provide job-type-specific metadata for BATCH (Spark) and STREAM (Flink)
 * executions.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
public sealed interface ExecutionMetadata permits BatchExecutionMetadata, StreamExecutionMetadata {

  /**
   * Returns the type discriminator for JSON serialization.
   *
   * @return "BATCH" or "STREAM"
   */
  String getType();
}
