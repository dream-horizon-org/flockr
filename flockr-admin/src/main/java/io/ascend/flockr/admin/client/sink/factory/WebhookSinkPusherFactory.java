package io.ascend.flockr.admin.client.sink.factory;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.sink.pusher.SinkPusher;
import io.ascend.flockr.admin.client.sink.pusher.WebhookSinkPusher;
import io.ascend.flockr.admin.client.webclient.WebClient;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating WebhookSinkPusher instances.
 *
 * <p>Since webhooks don't have expensive resources to cache (WebClient is shared), this factory
 * simply returns the same pusher instance for all requests.
 */
@Slf4j
public class WebhookSinkPusherFactory implements SinkPusherFactory {
  private static final String SINK_TYPE = "WEBHOOK";

  private final WebhookSinkPusher pusher;

  @Inject
  public WebhookSinkPusherFactory(WebClient webClient) {
    this.pusher = new WebhookSinkPusher(webClient);
    log.info("Created WebhookSinkPusherFactory with shared WebClient");
  }

  @Override
  public String getSinkType() {
    return SINK_TYPE;
  }

  @Override
  public SinkPusher create(DataSinkDetails sink) {
    // Return the same pusher instance - no need to cache since WebClient is already shared
    return pusher;
  }

  /** No-op close since WebClient lifecycle is managed separately. */
  public void close() {
    log.debug("WebhookSinkPusherFactory close called (no-op)");
  }
}
