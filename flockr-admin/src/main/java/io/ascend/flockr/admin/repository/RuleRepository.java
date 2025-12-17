package io.ascend.flockr.admin.repository;

import io.ascend.flockr.admin.domain.rule.RuleMeta;
import io.ascend.flockr.admin.domain.rule.RuleStatus;
import io.ascend.flockr.admin.domain.rule.SourceInfo;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

/**
 * Repository interface for accessing rule data from the database.
 *
 * <p>This repository provides methods for:
 *
 * <ul>
 *   <li>Creating rules in batch
 *   <li>Retrieving individual rules by ID
 *   <li>Retrieving all rules associated with an audience
 *   <li>Finding scheduled rules ready for execution
 *   <li>Updating rule status
 * </ul>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public interface RuleRepository {
  /**
   * Creates multiple rules in a single batch operation.
   *
   * @param ruleMetas the list of rule metadata objects containing rule definitions
   * @return a Single emitting true if all rules were created successfully
   */
  Single<Boolean> createRules(List<RuleMeta<SourceInfo>> ruleMetas);

  /**
   * Retrieves a rule by its unique identifier.
   *
   * @param xProjectId the encrypted project identifier
   * @param ruleId the unique rule identifier
   * @return a Single emitting the rule metadata with source information
   */
  Single<RuleMeta<SourceInfo>> getRuleById(String xProjectId, Long ruleId);

  /**
   * Retrieves all rules associated with a specific audience.
   *
   * @param xProjectId the encrypted project identifier
   * @param audienceId the audience identifier
   * @return a Single emitting a list of rule metadata objects ordered by creation date (newest
   *     first)
   */
  Single<List<RuleMeta<SourceInfo>>> getRulesByAudienceId(String xProjectId, Long audienceId);

  /**
   * Finds all rules that are scheduled and ready for execution. A rule is ready when:
   *
   * <ul>
   *   <li>status = SCHEDULED
   *   <li>start_time <= current time
   *   <li>end_time > current time
   * </ul>
   *
   * @return Single emitting list of rules ready for execution
   */
  Single<List<RuleMeta<SourceInfo>>> findScheduledRulesReadyForExecution();

  /**
   * Updates the status of a rule.
   *
   * @param ruleId the rule ID
   * @param status the new status
   * @return Completable that completes when update is done
   */
  Completable updateStatus(Long ruleId, RuleStatus status);

  /**
   * Updates rule status with optimistic locking using current status. Only updates if the current
   * status matches the expected value.
   *
   * @param ruleId the rule ID
   * @param currentStatus expected current status
   * @param newStatus the new status
   * @return Single<Boolean> true if updated, false if status didn't match
   */
  Single<Boolean> updateStatusIfCurrent(
      Long ruleId, RuleStatus currentStatus, RuleStatus newStatus);
}
