package io.ascend.flockr.admin.client.sink;

import io.ascend.flockr.admin.client.sink.factory.SinkPusherFactory;
import io.ascend.flockr.admin.client.sink.pusher.SinkPusher;
import io.ascend.flockr.admin.domain.audience.AudienceMeta;
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
 * Registry that manages all sink pusher factories and routes push requests to the appropriate
 * implementation.
 */
@Slf4j
public class SinkPusherRegistry {

  private final Map<String, SinkPusherFactory> factoriesByType;

  public SinkPusherRegistry(Set<SinkPusherFactory> factories) {
    this.factoriesByType =
        factories.stream()
            .collect(Collectors.toMap(SinkPusherFactory::getSinkType, Function.identity()));
    log.info(
        "Initialized SinkPusherRegistry with {} factories: {}",
        factories.size(),
        factoriesByType.keySet());
  }

  /**
   * Pushes a batch of audience records to the specified sink.
   *
   * @param records list of audience records
   * @param sink the target sink
   * @param audience the audience metadata
   * @return Completable that completes when push is done
   */
  public Completable pushBatch(
      List<AudienceRecord> records, DataSinkDetails sink, AudienceMeta audience) {
    String sinkType = sink.getType();
    SinkPusherFactory factory = factoriesByType.get(sinkType);

    if (factory == null) {
      log.warn("No factory found for sink type '{}', skipping sink '{}'", sinkType, sink.getName());
      return Completable.complete();
    }

    SinkPusher pusher = factory.create(sink);
    return pusher.pushBatch(records, sink, audience);
  }

  /**
   * Pushes a batch of audience records to all the specified sinks.
   *
   * @param records list of audience records
   * @param sinks list of target sinks
   * @param audience the audience metadata
   * @return Completable that completes when all pushes are done
   */
  public Completable pushBatchToAll(
      List<AudienceRecord> records, List<DataSinkDetails> sinks, AudienceMeta audience) {
    if (sinks == null || sinks.isEmpty()) {
      log.debug("No sinks configured for audience {}, skipping push", audience.getAudienceId());
      return Completable.complete();
    }

    // Push to all sinks in parallel
    List<Completable> pushOps =
        sinks.stream().map(sink -> pushBatch(records, sink, audience)).collect(Collectors.toList());

    return Completable.merge(pushOps);
  }

  /** Returns the set of supported sink types. */
  public Set<String> getSupportedTypes() {
    return factoriesByType.keySet();
  }
}
