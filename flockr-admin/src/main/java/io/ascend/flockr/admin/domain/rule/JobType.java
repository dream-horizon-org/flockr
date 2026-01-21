package io.ascend.flockr.admin.domain.rule;

/**
 * Type of job execution corresponding to the rule type.
 *
 * @author Flockr Team
 * @since 1.0
 */
public enum JobType {
  /** Real-time stream processing job using Flink CEP. */
  STREAM,

  /** Batch processing job using Spark SQL. */
  BATCH
}
