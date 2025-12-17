package io.ascend.flockr.admin.handlers;

import io.ascend.flockr.admin.config.ApplicationConfig;
import io.ascend.flockr.admin.repository.ExecutionSync;
import io.vertx.core.Handler;
import io.vertx.rxjava3.core.Vertx;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for scheduled handlers with distributed lease-based synchronization.
 *
 * <p>This class provides the foundation for implementing scheduled tasks that run periodically
 * across a distributed cluster. It ensures that only one instance executes at a time through a
 * lease-based locking mechanism.
 *
 * <p><b>Thread Safety:</b> The {@code handlerState} field is volatile to ensure visibility across
 * threads. Subclasses should be careful when accessing or modifying shared state.
 *
 * <p><b>State Management:</b> The handler transitions through states defined by {@link
 * HandlerState}. Subclasses should call {@link #checkAndUpdateState()} at the beginning of their
 * {@link #handle(Long)} implementation.
 *
 * @author Prithu Sharma
 * @since 1.0
 * @see ExecutionSync
 * @see HandlerState
 */
@Getter
@Slf4j
public abstract sealed class AbstractHandler implements Handler<Long> permits ExecuteRuleHandler {

  /** Current execution state of the handler, volatile for thread-safe visibility */
  protected volatile HandlerState handlerState;

  /** Vert.x instance for scheduling and async operations */
  protected final Vertx vertx;

  /** Distributed synchronization mechanism for lease acquisition */
  protected final ExecutionSync executionSync;

  /** Configuration for scheduling parameters */
  protected final ApplicationConfig.VertxSchedulerConfig config;

  /**
   * Constructs an AbstractHandler with the specified configuration.
   *
   * @param handlerState the initial handler state
   * @param vertx the Vert.x instance for async operations
   * @param executionSync the distributed synchronization mechanism
   * @param config the scheduler configuration containing timing and lease parameters
   */
  protected AbstractHandler(
      HandlerState handlerState,
      Vertx vertx,
      ExecutionSync executionSync,
      ApplicationConfig.VertxSchedulerConfig config) {
    this.handlerState = handlerState;
    this.vertx = vertx;
    this.executionSync = executionSync;
    this.config = config;
  }

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
