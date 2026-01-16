package io.ascend.flockr.admin.domain.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing a rule execution.
 *
 * <p>A RuleExecution tracks a single execution instance of a rule. Multiple executions can exist
 * for the same rule (e.g., for cron-based recurring rules or retries).
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleExecution {

  /** Unique identifier for the execution. */
  private Long executionId;

  /** Human-readable job name for tracking and monitoring. */
  private String jobName;

  /** The rule ID this execution belongs to. */
  private Long ruleId;

  /** Type of execution (STREAM or BATCH). */
  private JobType executionType;

  /** Current status of the execution. */
  private JobStatus status;

  /** Additional metadata about the execution (JSON). */
  private JsonObject metadata;

  /** External job reference ID (Spark submissionId or Flink jobId). */
  private String externalJobId;

  /** Number of reconciliation retry attempts for unmatched SUBMITTING jobs. */
  private Integer reconciliationRetries;

  /** Who/what created this execution. */
  private String createdBy;

  /** When the execution was created. */
  private Instant createdAt;

  /** When the execution was last updated. */
  private Instant updatedAt;
}
