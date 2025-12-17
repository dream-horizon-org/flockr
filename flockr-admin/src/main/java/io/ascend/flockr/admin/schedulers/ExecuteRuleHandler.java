package io.ascend.flockr.admin.schedulers;

import com.google.inject.Inject;
import io.ascend.flockr.admin.config.ApplicationConfig;
import io.ascend.flockr.admin.repository.RuleRepository;
import io.ascend.flockr.admin.service.job.JobServiceRegistry;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for executing scheduled rules with distributed lease-based synchronization.
 *
 * <p>This handler periodically:
 *
 * <ol>
 *   <li>Acquires a distributed lease (ensuring only one instance runs across the cluster)
 *   <li>Queries for rules with status=SCHEDULED and start_time <= now and end_time > now
 *   <li>Delegates execution to the appropriate job service via JobServiceRegistry
 * </ol>
 *
 * <p>The handler remains THIN - it only handles scheduling and lease acquisition. All execution
 * logic (record creation, status updates, job submission) is delegated to job services.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
public non-sealed class ExecuteRuleHandler extends AbstractHandler {

  private final RuleRepository ruleRepository;
  private final JobServiceRegistry jobServiceRegistry;

  @Inject
  public ExecuteRuleHandler(
      Vertx vertx,
      RuleRepository ruleRepository,
      JobServiceRegistry jobServiceRegistry,
      ApplicationConfig config,
      ExecutionSync executionSync) {

    this.vertx = vertx;
    this.ruleRepository = ruleRepository;
    this.jobServiceRegistry = jobServiceRegistry;
    this.config = config.getExecuteRuleHandler();
    this.executionSync = executionSync;
    this.handlerState = HandlerState.WAITING_TRIGGER;
  }

  @Override
  public void handle(Long event) {
    if (!checkAndUpdateState()) {
      return;
    }

    Duration ttl = Duration.ofSeconds(config.getMinExecutionDelay());

    executionSync
        .acquire(config.getSchedulerKey(), ttl)
        .flatMapSingle(
            acquired -> {
              handlerState = HandlerState.RUNNING;
              log.info(
                  "Acquired lease for key={}, executing scheduled rules...",
                  config.getSchedulerKey());
              return executeScheduledRules();
            })
        .doFinally(
            () -> {
              handlerState = HandlerState.WAITING_TRIGGER;
              log.debug("Handler state reset to WAITING_TRIGGER");
            })
        .subscribe(
            count -> log.info("Scheduled rule execution completed. Executed {} rules", count),
            error -> log.error("Error during scheduled rule execution", error),
            () -> log.debug("No lease acquired, skipping this cycle"));
  }

  /**
   * Executes all scheduled rules that are ready for execution.
   *
   * <p>Iterates through scheduled rules and delegates each to the appropriate job service based on
   * rule type. Continues even if individual rules fail.
   *
   * @return Single containing the count of rules that were successfully triggered
   */
  private Single<Integer> executeScheduledRules() {
    AtomicInteger successCount = new AtomicInteger(0);

    return ruleRepository
        .findScheduledRulesReadyForExecution()
        .doOnSuccess(
            rules -> log.info("Found {} scheduled rules ready for execution", rules.size()))
        .flatMapObservable(Observable::fromIterable)
        .flatMapCompletable(
            rule ->
                jobServiceRegistry
                    .get(rule.getRuleType())
                    .execute(rule, "scheduler")
                    .doOnComplete(
                        () -> {
                          successCount.incrementAndGet();
                          log.info(
                              "Successfully triggered execution for ruleId={}", rule.getRuleId());
                        })
                    .onErrorComplete(
                        error -> {
                          log.error(
                              "Failed to execute ruleId={}: {}",
                              rule.getRuleId(),
                              error.getMessage());
                          return true; // Continue with next rule
                        }))
        .toSingleDefault(successCount.get())
        .map(ignored -> successCount.get());
  }
}
