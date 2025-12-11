package io.ascend.flockr.admin.service.schedulers;

import com.google.inject.Inject;
import io.ascend.flockr.admin.config.ApplicationConfig;
import io.ascend.flockr.admin.repository.RuleRepository;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for executing scheduled rules with distributed lease-based synchronization.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public non-sealed class ExecuteRuleHandler extends AbstractHandler {
  private final RuleRepository ruleRepository;

  @Inject
  public ExecuteRuleHandler(
      Vertx vertx,
      RuleRepository ruleRepository,
      ApplicationConfig.VertxSchedulerConfig config,
      ExecutionSync executionSync) {

    this.vertx = vertx;
    this.ruleRepository = ruleRepository;
    this.config = config;
    this.executionSync = executionSync;
    this.handlerState = HandlerState.WAITING_TRIGGER;
  }

  @Override
  public void handle(Long event) {
    if (!checkAndUpdateState()) {
      return;
    }

    Duration ttl = Duration.ofMillis(config.getMinExecutionDelay());

    executionSync
        .acquire(config.getSchedulerKey(), ttl)
        .flatMapSingle(
            acquired -> {
              handlerState = HandlerState.RUNNING;
              log.info("Acquired lease, executing rules...");
              return executeRules();
            })
        .doFinally(() -> handlerState = HandlerState.WAITING_TRIGGER)
        .subscribe(
            result -> log.debug("Execution completed"),
            error -> log.error("Error during rule execution", error));
  }

  private Single<Boolean> executeRules() {
    // TODO: Implement actual rule execution logic
    log.info("Executing rules");
    return Single.just(true);
  }
}
