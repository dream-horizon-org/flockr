package io.ascend.flockr.admin.schedulers;

import io.ascend.flockr.admin.config.ApplicationConfig;
import io.vertx.core.Handler;
import io.vertx.rxjava3.core.Vertx;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for scheduled handlers with distributed lease-based synchronization.
 *
 * <p>Handlers extending this class are guaranteed to execute on only one instance at a time across
 * a distributed deployment, using a lease-based locking mechanism.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
@Data
public abstract sealed class AbstractHandler implements Handler<Long> permits ExecuteRuleHandler {

  protected volatile HandlerState handlerState;
  protected Vertx vertx;
  protected ExecutionSync executionSync;
  protected ApplicationConfig.VertxSchedulerConfig config;

  /**
   * Checks and updates handler state before attempting to acquire a lease. Should be called at the
   * start of handle() method.
   *
   * @return true if handler is ready to proceed, false if should skip
   */
  protected boolean checkAndUpdateState() {
    if (!handlerState.equals(HandlerState.WAITING_TRIGGER)) {
      log.trace("Skipping execution - handler state is {}", handlerState);
      return false;
    }
    handlerState = HandlerState.ACQUIRING_LOCK;
    log.debug("Attempting to acquire lease for key={}", config.getSchedulerKey());
    return true;
  }

  /**
   * Registers this handler with the Vert.x periodic timer.
   *
   * @return the timer ID
   */
  public Long setPeriodicHandler() {
    return vertx.setPeriodic(config.getPeriodicDelayMs(), this);
  }
}
