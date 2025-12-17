package io.ascend.flockr.admin.repository;

import io.ascend.flockr.admin.domain.rule.JobStatus;
import io.ascend.flockr.admin.domain.rule.RuleExecution;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;

/**
 * Repository for managing rule executions.
 *
 * <p>This repository handles CRUD operations for the rule_executions table, which tracks individual
 * execution instances of rules.
 *
 * @author Flockr Team
 * @since 1.0
 */
public interface RuleExecutionRepository {

  /**
   * Creates a new rule execution record.
   *
   * @param ruleExecution the execution to create
   * @return Single containing the generated execution ID
   */
  Single<Long> create(RuleExecution ruleExecution);

  /**
   * Updates execution status and optionally the external job reference ID.
   *
   * @param executionId the execution ID
   * @param status new status
   * @param externalJobId external job ID (Spark submissionId or Flink jobId), can be null
   * @param startedAt execution start time, can be null
   * @return Completable that completes when update is done
   */
  Completable updateStatusAndExternalJobId(
      Long executionId, JobStatus status, String externalJobId, Instant startedAt);

  /**
   * Marks an execution as failed with error message.
   *
   * @param executionId the execution ID
   * @param errorMessage the error message
   * @return Completable that completes when update is done
   */
  Completable markFailed(Long executionId, String errorMessage);

  /**
   * Marks an execution as completed.
   *
   * @param executionId the execution ID
   * @return Completable that completes when update is done
   */
  Completable markCompleted(Long executionId);

  /**
   * Finds an execution by its ID.
   *
   * @param executionId the execution ID
   * @return Maybe containing the execution if found
   */
  Maybe<RuleExecution> findById(Long executionId);

  /**
   * Finds the latest execution for a rule.
   *
   * @param ruleId the rule ID
   * @return Maybe containing the latest execution if exists
   */
  Maybe<RuleExecution> findLatestByRuleId(Long ruleId);
}
