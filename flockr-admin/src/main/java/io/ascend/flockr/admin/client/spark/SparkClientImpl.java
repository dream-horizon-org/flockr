package io.ascend.flockr.admin.client.spark;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.spark.io.response.SparkJobStatusResponse;
import io.ascend.flockr.admin.client.spark.io.response.SparkJobSubmissionResponse;
import io.ascend.flockr.admin.client.webclient.WebClient;
import io.ascend.flockr.admin.config.SparkConfig;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.web.client.HttpRequest;
import io.vertx.rxjava3.ext.web.client.HttpResponse;
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
        .doOnError(
            error -> {
              log.error("Failed to submit job", error);
            })
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
            status -> {
              log.debug(
                  "Submission {} status: {}, ApplicationId: {}",
                  submissionId,
                  status.getStatus(),
                  status.getApplicationId());
            })
        .doOnError(
            error -> {
              log.error("Failed to get job status for submission {}", submissionId, error);
            });
  }

  @Override
  public Completable cancelJob(String submissionId) {
    log.info("Cancelling job with submissionId: {}", submissionId);
    return executeRequest(
            HttpMethod.POST, "/v1/submissions/kill/" + submissionId, null, "Failed to cancel job")
        .doOnSuccess(
            response -> {
              log.info("Job with submissionId {} cancelled successfully", submissionId);
            })
        .doOnError(
            error -> {
              log.error("Failed to cancel job with submissionId {}", submissionId, error);
            })
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
            logs -> {
              log.debug(
                  "Application {} logs retrieved. Length: {}",
                  applicationId,
                  logs != null ? logs.length() : 0);
            })
        .doOnError(
            error -> {
              log.error(
                  "Failed to get application logs for applicationId {}", applicationId, error);
            });
  }

  /**
   * Execute HTTP request to Spark REST API.
   *
   * <p>Follows the same pattern as FlinkClientImpl.executeRequest()
   */
  private Single<HttpResponse<Buffer>> executeRequest(
      HttpMethod method, String path, JsonObject body, String errorMessage) {

    HttpRequest<Buffer> request;

    // Prepare request based on HTTP method
    if (method == HttpMethod.GET) {
      request = webClient.prepareHttpGETRequest(sparkConfig.getHost(), sparkConfig.getPort(), path);
    } else if (method == HttpMethod.POST) {
      request =
          webClient.prepareHttpPOSTRequest(sparkConfig.getHost(), sparkConfig.getPort(), path);
    } else {
      // For other methods, use POST as fallback
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

  /**
   * Validate HTTP response and handle errors.
   *
   * <p>Follows the same pattern as FlinkClientImpl.validateResponse()
   */
  private Single<HttpResponse<Buffer>> validateResponse(
      HttpResponse<Buffer> response, String errorMessage) {

    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      return Single.just(response);
    } else {
      int statusCode = response.statusCode();
      String responseBody = response.bodyAsString();

      log.error(
          "Spark API error: {}. Status: {}, Body: {}", errorMessage, statusCode, responseBody);

      // Return specific exceptions based on status code
      if (statusCode == 404) {
        return Single.error(
            SparkException.jobNotFound(extractApplicationIdFromError(responseBody)));
      } else if (statusCode >= 500) {
        return Single.error(
            SparkException.connectionError(
                String.format("%s. Spark cluster may be unavailable", errorMessage)));
      } else {
        return Single.error(SparkException.apiError(errorMessage, statusCode, responseBody));
      }
    }
  }

  /** Extract submission ID or application ID from error response if available. */
  private String extractApplicationIdFromError(String responseBody) {
    // Try to extract submissionId first, then applicationId
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
