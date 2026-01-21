package io.ascend.flockr.admin.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ascend.flockr.admin.domain.rule.RuleType;
import io.ascend.flockr.admin.service.impl.BatchRuleExecutionEngine;
import java.util.EnumMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry that maps RuleType to the appropriate AsyncJobService implementation.
 *
 * <p>Uses Strategy Pattern to route job execution to the correct service based on rule type:
 *
 * <ul>
 *   <li>BATCH rules → BatchJobService (Spark)
 *   <li>STREAM rules → StreamJobService (Flink)
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
@Singleton
public class RuleExecutionEngineRegistry {

  private final EnumMap<RuleType, RuleExecutionEngine> services;

  @Inject
  public RuleExecutionEngineRegistry(BatchRuleExecutionEngine batchJobService) {
    this.services = new EnumMap<>(RuleType.class);
    services.put(RuleType.BATCH, batchJobService);
    log.info("JobServiceRegistry initialized with {} services", services.size());
  }

  /**
   * Gets the appropriate job service for the given rule type.
   *
   * @param ruleType the type of rule
   * @return the AsyncJobService implementation for that type
   * @throws IllegalArgumentException if no service is registered for the rule type
   */
  public RuleExecutionEngine get(RuleType ruleType) {
    RuleExecutionEngine service = services.get(ruleType);
    if (service == null) {
      throw new IllegalArgumentException("No job service registered for rule type: " + ruleType);
    }
    return service;
  }
}
