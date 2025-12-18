package io.ascend.flockr.admin.client.sink.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.sink.SinkPusher;
import io.ascend.flockr.admin.client.webclient.WebClient;
import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.audience.AudienceRecord;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.config.WebhookSinkConfig;
import io.ascend.flockr.admin.util.ConfigurationUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.web.client.HttpRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Sink pusher implementation for Webhooks/HTTP APIs.
 *
 * <p>Sends audience records as JSON payloads to configured HTTP endpoints. Supports custom headers
 * for authentication and configurable HTTP methods (POST/PUT).
 *
 * <p>The pusher automatically sets the x-project-key header from the audience's xProjectId,
 * enabling multi-tenant webhook endpoints to identify the project context.
 */
@Slf4j
public class WebhookSinkPusher implements SinkPusher {

  private static final String SINK_TYPE = "WEBHOOK";
  private static final String PROJECT_KEY_HEADER = "x-project-key";
  private static final DateTimeFormatter EXPIRE_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

  private final WebClient webClient;

  @Inject
  public WebhookSinkPusher(WebClient webClient) {
    this.webClient = webClient;
  }

  @Override
  public String getSinkType() {
    return SINK_TYPE;
  }

  @Override
  public Completable pushBatch(
      List<AudienceRecord> records, DataSinkDetails sink, AudienceMeta audience) {
    WebhookSinkConfig config =
        ConfigurationUtil.parseSinkConfig(sink.getConfig(), WebhookSinkConfig.class);

    Long audienceId = audience.getAudienceId();
    String xProjectId = audience.getXProjectId();

    if (Boolean.TRUE.equals(config.getBatchMode())) {
      // Send all records in a single batch request
      return sendBatchRequest(records, config, audienceId, xProjectId);
    } else {
      // Send each record individually
      return sendIndividualRequests(records, config, audienceId, xProjectId);
    }
  }

  /** Sends all records as a single batch JSON array. */
  private Completable sendBatchRequest(
      List<AudienceRecord> records, WebhookSinkConfig config, Long audienceId, String xProjectId) {

    JsonArray payload = buildBatchPayload(records);

    log.debug(
        "Sending batch of {} records to webhook {} for audience {}",
        records.size(),
        config.getUrl(),
        audienceId);

    return executeRequest(config, payload, xProjectId)
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
      List<AudienceRecord> records, WebhookSinkConfig config, Long audienceId, String xProjectId) {

    log.debug(
        "Sending {} individual records to webhook {} for audience {}",
        records.size(),
        config.getUrl(),
        audienceId);

    return Flowable.fromIterable(records)
        .flatMapCompletable(
            record -> {
              JsonObject payload = toWebhookPayload(record);
              return executeRequest(config, payload, xProjectId);
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
  private Completable executeRequest(WebhookSinkConfig config, Object payload, String xProjectId) {
    HttpRequest<Buffer> request = createRequest(config);

    // Add custom headers from config
    if (config.getHeaders() != null) {
      for (Map.Entry<String, String> header : config.getHeaders().entrySet()) {
        request.putHeader(header.getKey(), header.getValue());
      }
    }

    // Set content type
    request.putHeader("Content-Type", config.getContentType());

    // Add x-project-key header for multi-tenant endpoints (e.g., flockr-users batch mapping)
    if (xProjectId != null && !xProjectId.isBlank()) {
      request.putHeader(PROJECT_KEY_HEADER, xProjectId);
    }

    return webClient
        .execute(request, payload)
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
          case "PUT" -> webClient.prepareHttpPutAbsRequest(config.getUrl());
          case "PATCH" -> webClient.prepareHttpPatchAbsRequest(config.getUrl());
          default -> webClient.prepareHttpPostAbsRequest(config.getUrl()); // POST is default
        };

    return request.timeout(timeout);
  }

  /** Builds a batch payload with all records as a JSON array. */
  private JsonArray buildBatchPayload(List<AudienceRecord> records) {
    JsonArray recordsArray = new JsonArray();
    for (AudienceRecord record : records) {
      recordsArray.add(toWebhookPayload(record));
    }
    return recordsArray;
  }

  /** Transforms an AudienceRecord to the webhook-specific payload format. */
  private JsonObject toWebhookPayload(AudienceRecord record) {
    JsonObject payload = new JsonObject();

    // user_id as a number
    try {
      payload.put("user_id", Long.parseLong(record.getUserId()));
    } catch (NumberFormatException e) {
      // Fall back to string if not a valid number
      payload.put("user_id", record.getUserId());
    }

    // cohort_key from audienceName
    payload.put("cohort_key", record.getAudienceName());

    // action mapping: "add" -> "append", others pass through
    String action = record.getAction();
    if ("add".equalsIgnoreCase(action)) {
      payload.put("action", "append");
    } else {
      payload.put("action", action);
    }

    // expire_at formatted as "yyyy-MM-dd HH:mm:ss"
    if (record.getExpireDate() != null) {
      String formattedDate =
          EXPIRE_DATE_FORMATTER.format(Instant.ofEpochSecond(record.getExpireDate()));
      payload.put("expire_at", formattedDate);
    }

    return payload;
  }
}
