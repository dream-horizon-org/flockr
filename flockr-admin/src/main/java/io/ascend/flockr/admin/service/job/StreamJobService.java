package io.ascend.flockr.admin.service.job;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ascend.flockr.admin.domain.rule.RuleMeta;
import io.ascend.flockr.admin.domain.rule.SourceInfo;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AsyncJobService implementation for STREAM rules using Flink.
 *
 * <p>Stub implementation - Flink support will be added in a future iteration.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class StreamJobService implements AsyncJobService {

  @Override
  public Completable execute(RuleMeta<SourceInfo> rule, String triggeredBy) {
    log.warn(
        "Stream execution not yet implemented. Rule: {}, triggeredBy: {}",
        rule.getRuleId(),
        triggeredBy);

    return Completable.error(
        new UnsupportedOperationException(
            "Stream job execution via Flink is not yet implemented. Please use BATCH rules."));
  }

  @Override
  public Completable cancelJob(String externalJobId) {
    log.warn("Stream job cancellation not yet implemented. ExternalJobId: {}", externalJobId);

    return Completable.error(
        new UnsupportedOperationException(
            "Stream job cancellation via Flink is not yet implemented."));
  }
}
