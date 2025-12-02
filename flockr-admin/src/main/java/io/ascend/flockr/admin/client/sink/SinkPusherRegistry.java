package io.ascend.flockr.admin.client.sink;

import io.ascend.flockr.admin.domain.audience.AudienceRecord;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry that manages all sink pushers and routes push requests to the appropriate
 * implementation.
 */
@Slf4j
public class SinkPusherRegistry {

  private final Map<String, SinkPusher> pushersByType;

  public SinkPusherRegistry(Set<SinkPusher> pushers) {
    this.pushersByType =
        pushers.stream().collect(Collectors.toMap(SinkPusher::getSinkType, Function.identity()));
    log.info(
        "Initialized SinkPusherRegistry with {} pushers: {}",
        pushers.size(),
        pushersByType.keySet());
  }

  /**
   * Pushes a batch of audience records to the specified sink.
   *
   * @param records list of audience records
   * @param sink the target sink
   * @param audienceId the audience ID
   * @return Completable that completes when push is done
   */
  public Completable pushBatch(
      List<AudienceRecord> records, DataSinkDetails sink, Long audienceId) {
    String sinkType = sink.getType();
    SinkPusher pusher = pushersByType.get(sinkType);

    if (pusher == null) {
      log.warn("No pusher found for sink type '{}', skipping sink '{}'", sinkType, sink.getName());
      return Completable.complete();
    }

    return pusher.pushBatch(records, sink, audienceId);
  }

  /**
   * Pushes a batch of audience records to all the specified sinks.
   *
   * @param records list of audience records
   * @param sinks list of target sinks
   * @param audienceId the audience ID
   * @return Completable that completes when all pushes are done
   */
  public Completable pushBatchToAll(
      List<AudienceRecord> records, List<DataSinkDetails> sinks, Long audienceId) {
    if (sinks == null || sinks.isEmpty()) {
      log.debug("No sinks configured for audience {}, skipping push", audienceId);
      return Completable.complete();
    }

    // Push to all sinks in parallel
    List<Completable> pushOps =
        sinks.stream()
            .map(sink -> pushBatch(records, sink, audienceId))
            .collect(Collectors.toList());

    return Completable.merge(pushOps);
  }

  /** Returns the set of supported sink types. */
  public Set<String> getSupportedTypes() {
    return pushersByType.keySet();
  }
}
