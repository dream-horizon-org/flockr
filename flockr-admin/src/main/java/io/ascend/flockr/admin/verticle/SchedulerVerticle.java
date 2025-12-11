package io.ascend.flockr.admin.verticle;

import com.google.inject.Inject;
import io.ascend.flockr.admin.schedulers.AbstractHandler;
import io.ascend.flockr.admin.schedulers.HandlerState;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.core.AbstractVerticle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Verticle responsible for managing scheduled handlers.
 *
 * <p>Registers all injected handlers with Vert.x periodic timers on startup and cancels them on
 * shutdown.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SchedulerVerticle extends AbstractVerticle {

  private final Set<AbstractHandler> handlers;
  private final List<Long> handlerTimerIds = new ArrayList<>();

  @Override
  public Completable rxStart() {
    log.info("Starting SchedulerVerticle with {} handlers", handlers.size());
    handlers.forEach(
        handler -> {
          Long timerId = handler.setPeriodicHandler();
          handlerTimerIds.add(timerId);
          log.info(
              "Registered handler {} with timer ID {}",
              handler.getClass().getSimpleName(),
              timerId);
        });
    return Completable.complete();
  }

  @Override
  public Completable rxStop() {
    log.info("Stopping SchedulerVerticle, cancelling {} timers", handlerTimerIds.size());
    handlerTimerIds.forEach(id -> vertx.cancelTimer(id));
    handlers.forEach(
        handler -> {
          long elapsedTimeSeconds = 0L;
          while (!handler.getHandlerState().equals(HandlerState.WAITING_TRIGGER)
              && elapsedTimeSeconds < handler.getConfig().getMinExecutionDelay()) {
            try {
              Thread.sleep(1000);
            } catch (Exception e) {
              log.error("Error while waiting for handler to complete", e);
            }
            elapsedTimeSeconds += 1;
          }
          if (elapsedTimeSeconds >= handler.getConfig().getMinExecutionDelay()) {
            log.error(
                "Handler did not complete within the minimum execution delay, cancelling timer");
          }
        });
    return Completable.complete();
  }
}
