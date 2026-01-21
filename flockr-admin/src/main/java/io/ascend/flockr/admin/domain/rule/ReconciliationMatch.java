package io.ascend.flockr.admin.domain.rule;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/**
 * Job-type agnostic representation of a matched external job for reconciliation.
 *
 * <p>This DTO decouples the domain/repository layer from specific execution engine types
 * (Spark/Flink). The service layer converts engine-specific responses to this common format.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Data
@Builder
public class ReconciliationMatch {

  /** The execution ID from the database. */
  private Long executionId;

  /** The rule ID associated with this execution. */
  private Long ruleId;

  /** External job identifier (Spark applicationId or Flink jobId). */
  private String externalJobId;

  /**
   * Job state from the external engine.
   *
   * <p>Common values:
   *
   * <ul>
   *   <li>Spark: RUNNING, FINISHED, FAILED, KILLED
   *   <li>Flink: RUNNING, FINISHED, FAILED, CANCELED
   * </ul>
   */
  private String state;

  /** When the job started in the external engine. */
  private Instant startedAt;
}
