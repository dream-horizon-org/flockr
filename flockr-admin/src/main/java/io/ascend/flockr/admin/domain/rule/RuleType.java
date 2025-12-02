package io.ascend.flockr.admin.domain.rule;

/**
 * Enumeration of rule execution types.
 *
 * <p>Rules can be executed in different modes depending on the data source and processing
 * requirements.
 *
 * @author Flockr Team
 * @since 1.0
 */
public enum RuleType {
  /** Real-time event stream processing using pattern matching (e.g., Flink CEP). */
  STREAM,

  /** Periodic batch processing using SQL queries executed on a schedule. */
  BATCH
}
