package io.ascend.flockr.admin.client.spark;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.spark.io.response.SparkApplicationInfo;
import io.ascend.flockr.admin.client.spark.io.response.SparkJobStatusResponse;
import io.ascend.flockr.admin.client.spark.io.response.SparkJobSubmissionResponse;
import io.ascend.flockr.admin.client.webclient.WebClient;
import io.ascend.flockr.admin.config.SparkConfig;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.web.client.HttpRequest;
import io.vertx.rxjava3.ext.web.client.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SparkClientImpl implements SparkClient {

  private static final String CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_JSON = "application/json";
  private static final String SUBMISSION_ID_FIELD = "submissionId";
  private static final String APPLICATION_ID_FIELD = "applicationId";
  private static final String STATUS_FIELD = "status";

  private final WebClient webClient;
  private final SparkConfig sparkConfig;

  @Inject
  public SparkClientImpl(SparkConfig sparkConfig, WebClient webClient) {
    this.sparkConfig = sparkConfig;
    this.webClient = webClient;
    log.info("SparkClient initialized for {}", sparkConfig.getBaseUrl());
  }

  @Override
  public Completable close() {
    return Completable.fromAction(
        () -> {
          webClient.close();
          log.info("SparkClient closed");
        });
  }

  @Override
  public Single<SparkJobSubmissionResponse> submit(JsonObject requestBody) {
    log.info("Submitting historicBatchJob request {}", requestBody.toString());
    return executeRequest(
            HttpMethod.POST, "/v1/submissions/create", requestBody, "Failed to submit job")
        .map(
            response -> {
              JsonObject responseBody = response.bodyAsJsonObject();
              String submissionId = responseBody.getString(SUBMISSION_ID_FIELD);
              log.info("Job submitted successfully. SubmissionId: {}", submissionId);
              return SparkJobSubmissionResponse.builder().submissionId(submissionId).build();
            })
        .doOnError(error -> log.error("Failed to submit job", error))
        .onErrorResumeNext(
            error -> Single.error(SparkException.submissionError("Job submission failed", error)));
  }

  @Override
  public Single<SparkJobStatusResponse> getJobStatus(String submissionId) {
    log.debug("Getting status for submission: {}", submissionId);
    return executeRequest(
            HttpMethod.GET,
            "/v1/submissions/status/" + submissionId,
            null,
            "Failed to get job status")
        .map(
            response -> {
              JsonObject statusJson = response.bodyAsJsonObject();
              return SparkJobStatusResponse.builder()
                  .applicationId(statusJson.getString(APPLICATION_ID_FIELD))
                  .status(statusJson.getString(STATUS_FIELD))
                  .message(statusJson.getString("message"))
                  .driverState(statusJson.getString("driverState"))
                  .workerId(statusJson.getString("workerId"))
                  .build();
            })
        .doOnSuccess(
            status ->
                log.debug(
                    "Submission {} status: {}, ApplicationId: {}",
                    submissionId,
                    status.getStatus(),
                    status.getApplicationId()))
        .doOnError(
            error -> log.error("Failed to get job status for submission {}", submissionId, error));
  }

  @Override
  public Completable cancelJob(String submissionId) {
    log.info("Cancelling job with submissionId: {}", submissionId);
    return executeRequest(
            HttpMethod.POST, "/v1/submissions/kill/" + submissionId, null, "Failed to cancel job")
        .doOnSuccess(
            response -> log.info("Job with submissionId {} cancelled successfully", submissionId))
        .doOnError(
            error -> log.error("Failed to cancel job with submissionId {}", submissionId, error))
        .ignoreElement();
  }

  @Override
  public Single<String> getApplicationLogs(String applicationId) {
    log.debug("Getting application logs for applicationId {}", applicationId);
    return executeRequest(
            HttpMethod.GET,
            "/v1/applications/" + applicationId + "/logs",
            null,
            "Failed to get application logs for applicationId: " + applicationId)
        .map(HttpResponse::bodyAsString)
        .doOnSuccess(
            logs ->
                log.debug(
                    "Application {} logs retrieved. Length: {}",
                    applicationId,
                    logs != null ? logs.length() : 0))
        .doOnError(
            error ->
                log.error(
                    "Failed to get application logs for applicationId {}", applicationId, error));
  }

  // ==================== Spark Master Web UI Methods ====================

  @Override
  public Single<List<SparkApplicationInfo>> listApplications(
      String status, Instant minDate, Instant maxDate, Integer limit) {

    // Use Spark Master Web UI REST API endpoint
    String path = "/json/";
    log.debug("Listing applications from Spark Master Web UI: {}", path);

    return executeWebUIRequest(path)
        .map(response -> parseApplicationListFromWebUI(response, status, minDate, maxDate, limit))
        .doOnSuccess(apps -> log.debug("Found {} applications from Spark Master", apps.size()))
        .doOnError(e -> log.error("Failed to list applications from Spark Master", e));
  }

  @Override
  public Single<List<SparkApplicationInfo>> findApplicationsByNamePattern(
      String namePattern, Instant minDate, Instant maxDate) {

    // Convert glob pattern to regex
    String regex = "^" + namePattern.replace(".", "\\.").replace("*", ".*").replace("?", ".") + "$";
    Pattern pattern = Pattern.compile(regex);

    log.debug("Finding applications matching pattern: {} (regex: {})", namePattern, regex);

    return listApplications(null, minDate, maxDate, null)
        .map(
            apps ->
                apps.stream()
                    .filter(
                        app -> app.getName() != null && pattern.matcher(app.getName()).matches())
                    .toList())
        .doOnSuccess(
            matches ->
                log.debug(
                    "Found {} applications matching pattern {}", matches.size(), namePattern));
  }

  // ==================== Private Helper Methods ====================

  /** Execute HTTP request to Spark Master REST API. */
  private Single<HttpResponse<Buffer>> executeRequest(
      HttpMethod method, String path, JsonObject body, String errorMessage) {

    HttpRequest<Buffer> request;

    if (method == HttpMethod.GET) {
      request = webClient.prepareHttpGETRequest(sparkConfig.getHost(), sparkConfig.getPort(), path);
    } else {
      request =
          webClient.prepareHttpPOSTRequest(sparkConfig.getHost(), sparkConfig.getPort(), path);
    }

    request.timeout(sparkConfig.getRequestTimeout());

    if (body != null) {
      request.putHeader(CONTENT_TYPE, APPLICATION_JSON);
      return request
          .rxSendJsonObject(body)
          .flatMap(response -> validateResponse(response, errorMessage));
    } else {
      return request.rxSend().flatMap(response -> validateResponse(response, errorMessage));
    }
  }

  /** Execute GET request to Spark Master Web UI (port 8080 by default for web UI). */
  private Single<HttpResponse<Buffer>> executeWebUIRequest(String path) {
    // Spark Master Web UI is typically on port 8080, not the REST submission port (6066)
    int webUIPort = 8080;

    HttpRequest<Buffer> request =
        webClient.prepareHttpGETRequest(sparkConfig.getHost(), webUIPort, path);
    request.timeout(sparkConfig.getRequestTimeout());

    return request
        .rxSend()
        .flatMap(
            response -> {
              if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Single.just(response);
              }
              log.error(
                  "Spark Master Web UI error: status={}, body={}",
                  response.statusCode(),
                  response.bodyAsString());
              return Single.error(
                  SparkException.apiError(
                      "Spark Master Web UI request failed",
                      response.statusCode(),
                      response.bodyAsString()));
            });
  }

  /**
   * Parse the JSON response from Spark Master Web UI into SparkApplicationInfo list.
   *
   * <p>Spark Master Web UI /json/ endpoint returns format:
   *
   * <pre>
   * {
   *   "activeapps": [...],
   *   "completedapps": [...]
   * }
   * </pre>
   *
   * Each app has: id, starttime, name, cores, user, memoryperslave, submitdate, state, duration
   */
  private List<SparkApplicationInfo> parseApplicationListFromWebUI(
      HttpResponse<Buffer> response,
      String statusFilter,
      Instant minDate,
      Instant maxDate,
      Integer limit) {

    JsonObject root = response.bodyAsJsonObject();
    List<SparkApplicationInfo> result = new ArrayList<>();

    // Parse active applications
    JsonArray activeApps = root.getJsonArray("activeapps");
    if (activeApps != null) {
      for (int i = 0; i < activeApps.size(); i++) {
        JsonObject appJson = activeApps.getJsonObject(i);
        SparkApplicationInfo app = parseWebUIApplication(appJson, "RUNNING");
        if (matchesFilters(app, statusFilter, minDate, maxDate)) {
          result.add(app);
        }
      }
    }

    // Parse completed applications
    JsonArray completedApps = root.getJsonArray("completedapps");
    if (completedApps != null) {
      for (int i = 0; i < completedApps.size(); i++) {
        JsonObject appJson = completedApps.getJsonObject(i);
        SparkApplicationInfo app = parseWebUIApplication(appJson, "FINISHED");
        if (matchesFilters(app, statusFilter, minDate, maxDate)) {
          result.add(app);
        }
      }
    }

    // Apply limit if specified
    if (limit != null && result.size() > limit) {
      result = result.subList(0, limit);
    }

    return result;
  }

  /** Parse a single application from Spark Master Web UI format. */
  private SparkApplicationInfo parseWebUIApplication(JsonObject appJson, String state) {
    // starttime is in milliseconds
    Long startTimeMs = appJson.getLong("starttime");
    Long duration = appJson.getLong("duration");

    Instant startTime = parseEpochMillis(startTimeMs);
    Instant endTime = null;

    // Calculate end time if we have start time and duration
    if (startTime != null && duration != null && duration > 0) {
      endTime = startTime.plusMillis(duration);
    }

    return SparkApplicationInfo.builder()
        .id(appJson.getString("id"))
        .name(appJson.getString("name"))
        .state(state)
        .startTime(startTime)
        .endTime(endTime)
        .duration(duration)
        .user(appJson.getString("user"))
        .build();
  }

  /** Check if an application matches the given filters. */
  private boolean matchesFilters(
      SparkApplicationInfo app, String statusFilter, Instant minDate, Instant maxDate) {

    // Status filter
    if (statusFilter != null && !statusFilter.equalsIgnoreCase(app.getState())) {
      return false;
    }

    // Date range filter
    if (app.getStartTime() != null) {
      if (minDate != null && app.getStartTime().isBefore(minDate)) {
        return false;
      }
      if (maxDate != null && app.getStartTime().isAfter(maxDate)) {
        return false;
      }
    }

    return true;
  }

  private Instant parseEpochMillis(Long epochMillis) {
    return epochMillis != null ? Instant.ofEpochMilli(epochMillis) : null;
  }

  private Single<HttpResponse<Buffer>> validateResponse(
      HttpResponse<Buffer> response, String errorMessage) {

    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      return Single.just(response);
    }

    int statusCode = response.statusCode();
    String responseBody = response.bodyAsString();

    log.error("Spark API error: {}. Status: {}, Body: {}", errorMessage, statusCode, responseBody);

    if (statusCode == 404) {
      return Single.error(SparkException.jobNotFound(extractIdFromError(responseBody)));
    } else if (statusCode >= 500) {
      return Single.error(
          SparkException.connectionError(
              String.format("%s. Spark cluster may be unavailable", errorMessage)));
    } else {
      return Single.error(SparkException.apiError(errorMessage, statusCode, responseBody));
    }
  }

  private String extractIdFromError(String responseBody) {
    try {
      JsonObject errorJson = new JsonObject(responseBody);
      String submissionId = errorJson.getString(SUBMISSION_ID_FIELD);
      if (submissionId != null) {
        return submissionId;
      }
      return errorJson.getString(APPLICATION_ID_FIELD, "unknown");
    } catch (Exception e) {
      return "unknown";
    }
  }
}
