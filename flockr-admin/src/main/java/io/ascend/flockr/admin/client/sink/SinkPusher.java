package io.ascend.flockr.admin.client.sink;

import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.audience.AudienceRecord;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;

/**
 * Interface for pushing data to different sink types.
 *
 * <p>Each sink type (KAFKA, S3_FOLDER, etc.) has its own implementation that handles the specific
 * protocol and configuration.
 */
public interface SinkPusher {

  /**
   * Returns the sink type this pusher handles.
   *
   * @return the sink type identifier (e.g., "KAFKA", "S3_FOLDER")
   */
  String getSinkType();

  /**
   * Pushes a batch of audience records to the sink.
   *
   * @param records list of audience records to push
   * @param sink the sink configuration details
   * @param audience the audience metadata containing context info (xProjectId, audienceId, etc.)
   * @return Completable that completes when push is done
   */
  Completable pushBatch(List<AudienceRecord> records, DataSinkDetails sink, AudienceMeta audience);
}
