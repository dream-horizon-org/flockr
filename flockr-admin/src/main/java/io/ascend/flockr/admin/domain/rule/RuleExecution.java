package io.ascend.flockr.admin.domain.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.List;
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

  /** Human-readable job name for tracking and monitoring. */
  private String jobName;

  /** Unique identifier for the execution. */
  private Long executionId;

  /** External job reference ID (Spark submissionId or Flink jobId). */
  private String externalJobId;

  /** The rule ID this execution belongs to. */
  private Long ruleId;

  /** List of sink IDs where output is sent. */
  private List<Long> sinkIds;

  /** Type of execution (STREAM or BATCH). */
  private JobType executionType;

  /** Current status of the execution. */
  private JobStatus status;

  /** Additional metadata about the execution (JSON). */
  private JsonObject metadata;

  /** Number of retry attempts, defaults to 0. */
  @Builder.Default private Integer retries = 0;

  /** Who/what triggered this execution. */
  private String triggeredBy;

  /** When the execution was created. */
  private Instant createdAt;

  /** When the execution was last updated. */
  private Instant updatedAt;
}
