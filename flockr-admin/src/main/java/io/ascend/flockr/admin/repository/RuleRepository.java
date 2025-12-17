package io.ascend.flockr.admin.repository;

import io.ascend.flockr.admin.domain.rule.*;
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
 *   <li>Finding scheduled rules ready for execution (domain-specific)
 *   <li>Finding rules by status (generic queries)
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
   * Finds all rules that are scheduled and ready for execution with their associated sink IDs. A
   * rule is ready when:
   *
   * <ul>
   *   <li>status = SCHEDULED
   *   <li>start_time <= current time
   *   <li>end_time > current time
   * </ul>
   *
   * <p>This method joins with the audiences table to fetch the sink IDs associated with each rule's
   * audience. The sink details can then be enriched separately using batch fetch operations.
   *
   * <p><b>This is a domain-specific method</b> that encapsulates business logic for finding "ready
   * to execute" rules.
   *
   * @return Single emitting list of rules with populated sink IDs ready for execution
   */
  Single<List<RuleMetaVerbose<SourceInfo, SinkInfo>>> findScheduledRulesReadyWithSinkIds();

  /**
   * Updates the status of a rule if it matches the current status.
   *
   * @param ruleId the unique rule identifier
   * @param newStatus the new status to set
   * @param currentStatus the expected current status
   * @return a Single emitting true if the status was updated, false otherwise
   */
  Single<Boolean> updateRuleStatus(Long ruleId, RuleStatus newStatus, RuleStatus currentStatus);
}
