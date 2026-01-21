package io.ascend.flockr.admin.client.sink.factory;

import io.ascend.flockr.admin.client.sink.pusher.SinkPusher;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;

/**
 * Factory interface for creating sink pushers.
 *
 * <p>Each factory implementation handles a specific sink type (KAFKA, S3_FOLDER, WEBHOOK, etc.) and
 * manages the lifecycle of pushers and their underlying resources.
 */
public interface SinkPusherFactory {
  /**
   * Returns the sink type this factory handles.
   *
   * @return the sink type identifier (e.g., "KAFKA", "S3_FOLDER", "WEBHOOK")
   */
  String getSinkType();

  /**
   * Creates or retrieves a cached pusher for the given sink.
   *
   * @param sinkDetails the sink configuration details
   * @return a SinkPusher instance
   */
  SinkPusher create(DataSinkDetails sinkDetails);
}
