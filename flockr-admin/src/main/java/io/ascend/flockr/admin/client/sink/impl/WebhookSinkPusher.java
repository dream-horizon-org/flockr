package io.ascend.flockr.admin.client.sink.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.sink.SinkPusher;
import io.ascend.flockr.admin.domain.audience.AudienceRecord;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.config.WebhookSinkConfig;
import io.ascend.flockr.admin.util.ConfigParser;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.web.client.HttpRequest;
import io.vertx.rxjava3.ext.web.client.WebClient;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Sink pusher implementation for Webhooks/HTTP APIs.
 *
 * <p>Sends audience records as JSON payloads to configured HTTP endpoints. Supports custom headers
 * for authentication and configurable HTTP methods (POST/PUT).
 */
@Slf4j
public class WebhookSinkPusher implements SinkPusher {

  private static final String SINK_TYPE = "WEBHOOK";

  private final WebClient webClient;

  @Inject
  public WebhookSinkPusher(Vertx vertx) {
    this.webClient = WebClient.create(vertx);
  }

  @Override
  public String getSinkType() {
    return SINK_TYPE;
  }

  @Override
  public Completable pushBatch(
      List<AudienceRecord> records, DataSinkDetails sink, Long audienceId) {
    WebhookSinkConfig config =
        ConfigParser.parseSinkConfig(sink.getConfig(), WebhookSinkConfig.class);

    if (Boolean.TRUE.equals(config.getBatchMode())) {
      // Send all records in a single batch request
      return sendBatchRequest(records, config, audienceId);
    } else {
      // Send each record individually
      return sendIndividualRequests(records, config, audienceId);
    }
  }

  /** Sends all records as a single batch JSON array. */
  private Completable sendBatchRequest(
      List<AudienceRecord> records, WebhookSinkConfig config, Long audienceId) {

    JsonObject payload = buildBatchPayload(records);

    log.debug(
        "Sending batch of {} records to webhook {} for audience {}",
        records.size(),
        config.getUrl(),
        audienceId);

    return executeRequest(config, payload)
        .doOnComplete(
            () ->
                log.info(
                    "Successfully sent {} records to webhook {} for audience {}",
                    records.size(),
                    config.getUrl(),
                    audienceId))
        .doOnError(
            error ->
                log.error(
                    "Failed to send batch to webhook {} for audience {}: {}",
                    config.getUrl(),
                    audienceId,
                    error.getMessage()));
  }

  /** Sends each record as an individual request. */
  private Completable sendIndividualRequests(
      List<AudienceRecord> records, WebhookSinkConfig config, Long audienceId) {

    log.debug(
        "Sending {} individual records to webhook {} for audience {}",
        records.size(),
        config.getUrl(),
        audienceId);

    return Flowable.fromIterable(records)
        .flatMapCompletable(
            record -> {
              JsonObject payload = record.toJson();
              return executeRequest(config, payload);
            },
            false,
            4) // Max 4 concurrent requests
        .doOnComplete(
            () ->
                log.info(
                    "Successfully sent {} individual records to webhook {} for audience {}",
                    records.size(),
                    config.getUrl(),
                    audienceId))
        .doOnError(
            error ->
                log.error(
                    "Failed to send records to webhook {} for audience {}: {}",
                    config.getUrl(),
                    audienceId,
                    error.getMessage()));
  }

  /** Executes the HTTP request with the configured settings. */
  private Completable executeRequest(WebhookSinkConfig config, JsonObject payload) {
    HttpRequest<Buffer> request = createRequest(config);

    // Add custom headers
    if (config.getHeaders() != null) {
      for (Map.Entry<String, String> header : config.getHeaders().entrySet()) {
        request.putHeader(header.getKey(), header.getValue());
      }
    }

    // Set content type
    request.putHeader("Content-Type", config.getContentType());

    return request
        .rxSendJsonObject(payload)
        .flatMapCompletable(
            response -> {
              int statusCode = response.statusCode();
              if (statusCode >= 200 && statusCode < 300) {
                log.debug("Webhook response: {} - {}", statusCode, response.statusMessage());
                return Completable.complete();
              } else {
                String errorBody = response.bodyAsString();
                log.warn(
                    "Webhook returned non-success status: {} - {}. Body: {}",
                    statusCode,
                    response.statusMessage(),
                    errorBody);
                return Completable.error(
                    new RuntimeException(
                        String.format(
                            "Webhook returned status %d: %s",
                            statusCode, response.statusMessage())));
              }
            });
  }

  /** Creates the HTTP request based on configured method. */
  private HttpRequest<Buffer> createRequest(WebhookSinkConfig config) {
    String method = config.getMethod() != null ? config.getMethod().toUpperCase() : "POST";
    int timeout = config.getTimeoutMs() != null ? config.getTimeoutMs() : 30000;

    HttpRequest<Buffer> request =
        switch (method) {
          case "PUT" -> webClient.putAbs(config.getUrl());
          case "PATCH" -> webClient.patchAbs(config.getUrl());
          default -> webClient.postAbs(config.getUrl()); // POST is default
        };

    return request.timeout(timeout);
  }

  /** Builds a batch payload with all records. */
  private JsonObject buildBatchPayload(List<AudienceRecord> records) {
    JsonArray recordsArray = new JsonArray();
    for (AudienceRecord record : records) {
      recordsArray.add(record.toJson());
    }

    return new JsonObject()
        .put("recordCount", records.size())
        .put("timestamp", System.currentTimeMillis())
        .put("records", recordsArray);
  }

  /** Closes the web client. Call this on application shutdown. */
  public void close() {
    log.info("Closing webhook sink web client");
    webClient.close();
  }
}
