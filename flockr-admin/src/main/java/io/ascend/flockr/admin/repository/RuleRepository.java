package io.ascend.flockr.admin.repository;

import com.ascend.flockr.domain.rule.RuleMeta;
import com.ascend.flockr.domain.rule.SourceInfo;
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
 * </ul>
 *
 * @author Flockr Team
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
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param ruleId the unique rule identifier
   * @return a Single emitting the rule metadata with source information
   */
  Single<RuleMeta<SourceInfo>> getRuleById(String tenantId, String projectId, Long ruleId);

  /**
   * Retrieves all rules associated with a specific audience.
   *
   * @param tenantId the tenant identifier
   * @param projectId the project identifier
   * @param audienceId the audience identifier
   * @return a Single emitting a list of rule metadata objects ordered by creation date (newest
   *     first)
   */
  Single<List<RuleMeta<SourceInfo>>> getRulesByAudienceId(
      String tenantId, String projectId, Long audienceId);
}
