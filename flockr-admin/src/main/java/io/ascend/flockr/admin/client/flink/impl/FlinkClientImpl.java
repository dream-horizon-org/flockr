package io.ascend.flockr.admin.client.flink.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.flink.FlinkClient;
import io.ascend.flockr.admin.client.webclient.WebClient;
import io.ascend.flockr.admin.config.FlinkConfig;
import io.ascend.flockr.admin.exception.FlinkApiException;
import io.ascend.flockr.admin.exception.FlinkConnectionException;
import io.ascend.flockr.admin.exception.FlinkJarUploadException;
import io.ascend.flockr.admin.exception.FlinkJobNotFoundException;
import io.ascend.flockr.admin.exception.FlinkJobSubmissionException;
import io.ascend.flockr.admin.exception.FlinkSavepointException;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.web.client.HttpRequest;
import io.vertx.rxjava3.ext.web.client.HttpResponse;
import io.vertx.rxjava3.ext.web.multipart.MultipartForm;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of FlinkClient that interacts with Apache Flink REST API. Uses Vert.x WebClient
 * for HTTP communication with the Flink cluster.
 */
@Slf4j
public class FlinkClientImpl implements FlinkClient {

  private static final String CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_JSON = "application/json";
  private static final String JOB_ID_FIELD = "jobid";
  private static final String STATUS_FIELD = "status";
  private static final String REQUEST_ID_FIELD = "request-id";
  private static final String LOCATION_FIELD = "location";
  private static final String FILENAME_FIELD = "filename";

  private final WebClient webClient;
  private final FlinkConfig flinkConfig;

  @Inject
  public FlinkClientImpl(FlinkConfig flinkConfig, WebClient webClient) {
    this.flinkConfig = flinkConfig;
    this.webClient = webClient;
    log.info("FlinkClient initialized for {}:{}", flinkConfig.getHost(), flinkConfig.getPort());
  }

  @Override
  public Completable close() {
    return Completable.fromAction(
        () -> {
          webClient.close();
          log.info("FlinkClient closed");
        });
  }

  @Override
  public Single<String> submitJob(
      String jarId,
      String entryClass,
      String programArgs,
      Integer parallelism,
      String savepointPath) {
    log.info("Submitting job with jarId: {}, entryClass: {}", jarId, entryClass);

    JsonObject requestBody = new JsonObject();
    if (entryClass != null && !entryClass.isEmpty()) {
      requestBody.put("entryClass", entryClass);
    }
    if (programArgs != null && !programArgs.isEmpty()) {
      requestBody.put("programArgs", programArgs);
    }
    if (parallelism != null) {
      requestBody.put("parallelism", parallelism);
    }
    if (savepointPath != null && !savepointPath.isEmpty()) {
      requestBody.put("savepointPath", savepointPath);
    }

    return executeRequest(
            HttpMethod.POST, "/jars/" + jarId + "/run", requestBody, "Failed to submit job")
        .map(
            response -> {
              String jobId = response.bodyAsJsonObject().getString(JOB_ID_FIELD);
              log.info("Job submitted successfully with jobId: {}", jobId);
              return jobId;
            })
        .doOnError(
            error -> {
              log.error("Failed to submit job", error);
            })
        .onErrorResumeNext(
            error -> Single.error(new FlinkJobSubmissionException(jarId, entryClass, error)));
  }

  @Override
  public Single<String> uploadJar(String jarFilePath) {
    log.info("Uploading JAR file: {}", jarFilePath);

    MultipartForm form =
        MultipartForm.create()
            .binaryFileUpload(
                "jarfile",
                Paths.get(jarFilePath).getFileName().toString(),
                jarFilePath,
                APPLICATION_JSON);

    return webClient
        .prepareHttpPOSTRequest(flinkConfig.getHost(), flinkConfig.getPort(), "/jars/upload")
        .timeout(flinkConfig.getRequestTimeout())
        .rxSendMultipartForm(form)
        .map(
            response -> {
              if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonObject jsonResponse = response.bodyAsJsonObject();
                String filename = jsonResponse.getString(FILENAME_FIELD);
                // Extract JAR ID from filename (format: /jars/jar-id_filename.jar)
                String jarId = filename.substring(filename.lastIndexOf('/') + 1);
                log.info("JAR uploaded successfully with ID: {}", jarId);
                return jarId;
              } else {
                log.error("Failed to upload JAR. Status: {}", response.statusCode());
                throw new FlinkJarUploadException(jarFilePath, response.statusCode());
              }
            })
        .doOnError(
            error -> {
              log.error("Error uploading JAR", error);
            })
        .onErrorResumeNext(
            error -> {
              if (error instanceof FlinkJarUploadException) {
                return Single.error(error);
              }
              return Single.error(
                  new FlinkJarUploadException(
                      String.format("Failed to upload JAR file: %s", jarFilePath), error));
            });
  }

  @Override
  public Single<JsonObject> getJobStatus(String jobId) {
    log.debug("Getting status for job: {}", jobId);
    return executeRequest(HttpMethod.GET, "/jobs/" + jobId, null, "Failed to get job status")
        .map(HttpResponse::bodyAsJsonObject)
        .doOnSuccess(
            status -> log.debug("Job {} status: {}", jobId, status.getString(STATUS_FIELD)));
  }

  @Override
  public Completable cancelJob(String jobId) {
    log.info("Cancelling job: {}", jobId);
    return executeRequest(HttpMethod.PATCH, "/jobs/" + jobId, null, "Failed to cancel job")
        .doOnSuccess(
            response -> {
              log.info("Job {} cancelled successfully", jobId);
            })
        .doOnError(
            error -> {
              log.error("Failed to cancel job {}", jobId, error);
            })
        .ignoreElement();
  }

  @Override
  public Single<String> cancelJobWithSavepoint(String jobId, String savepointDirectory) {
    log.info("Cancelling job {} with savepoint to: {}", jobId, savepointDirectory);

    JsonObject requestBody =
        new JsonObject().put("targetDirectory", savepointDirectory).put("cancelJob", true);

    return executeRequest(
            HttpMethod.POST,
            "/jobs/" + jobId + "/savepoints",
            requestBody,
            "Failed to cancel job with savepoint")
        .flatMap(
            response -> {
              String requestId = response.bodyAsJsonObject().getString(REQUEST_ID_FIELD);
              return pollSavepointStatus(jobId, requestId);
            })
        .doOnSuccess(
            savepointPath -> {
              log.info("Job {} cancelled with savepoint at: {}", jobId, savepointPath);
            })
        .doOnError(
            error -> {
              log.error("Failed to cancel job {} with savepoint", jobId, error);
            })
        .onErrorResumeNext(
            error -> {
              if (error instanceof FlinkSavepointException) {
                return Single.error(error);
              }
              return Single.error(
                  new FlinkSavepointException(jobId, "cancel with savepoint", error.getMessage()));
            });
  }

  @Override
  public Single<String> triggerSavepoint(String jobId, String savepointDirectory) {
    log.info("Triggering savepoint for job {} to: {}", jobId, savepointDirectory);

    JsonObject requestBody =
        new JsonObject().put("targetDirectory", savepointDirectory).put("cancelJob", false);

    return executeRequest(
            HttpMethod.POST,
            "/jobs/" + jobId + "/savepoints",
            requestBody,
            "Failed to trigger savepoint")
        .flatMap(
            response -> {
              String requestId = response.bodyAsJsonObject().getString(REQUEST_ID_FIELD);
              return pollSavepointStatus(jobId, requestId);
            })
        .doOnSuccess(
            savepointPath -> {
              log.info("Savepoint triggered successfully at: {}", savepointPath);
            })
        .doOnError(
            error -> {
              log.error("Failed to trigger savepoint for job {}", jobId, error);
            })
        .onErrorResumeNext(
            error -> {
              if (error instanceof FlinkSavepointException) {
                return Single.error(error);
              }
              return Single.error(
                  new FlinkSavepointException(jobId, "trigger", error.getMessage()));
            });
  }

  @Override
  public Single<JsonArray> listJobs() {
    log.debug("Listing all jobs");
    return executeRequest(HttpMethod.GET, "/jobs", null, "Failed to list jobs")
        .map(response -> response.bodyAsJsonObject().getJsonArray("jobs", new JsonArray()));
  }

  @Override
  public Single<JsonObject> getJobDetails(String jobId) {
    log.debug("Getting details for job: {}", jobId);
    return executeRequest(HttpMethod.GET, "/jobs/" + jobId, null, "Failed to get job details")
        .map(HttpResponse::bodyAsJsonObject);
  }

  @Override
  public Single<JsonObject> getClusterOverview() {
    log.debug("Getting cluster overview");
    return executeRequest(HttpMethod.GET, "/overview", null, "Failed to get cluster overview")
        .map(HttpResponse::bodyAsJsonObject);
  }

  @Override
  public Single<JsonArray> listTaskManagers() {
    log.debug("Listing task managers");
    return executeRequest(HttpMethod.GET, "/taskmanagers", null, "Failed to list task managers")
        .map(response -> response.bodyAsJsonObject().getJsonArray("taskmanagers", new JsonArray()));
  }

  @Override
  public Single<JsonObject> getJobExecutionPlan(String jobId) {
    log.debug("Getting execution plan for job: {}", jobId);
    return executeRequest(
            HttpMethod.GET, "/jobs/" + jobId + "/plan", null, "Failed to get job execution plan")
        .map(HttpResponse::bodyAsJsonObject);
  }

  @Override
  public Single<JsonObject> getJobExceptions(String jobId) {
    log.debug("Getting exceptions for job: {}", jobId);
    return executeRequest(
            HttpMethod.GET, "/jobs/" + jobId + "/exceptions", null, "Failed to get job exceptions")
        .map(HttpResponse::bodyAsJsonObject);
  }

  @Override
  public Single<JsonObject> getJobMetrics(String jobId) {
    log.debug("Getting metrics for job: {}", jobId);
    return executeRequest(
            HttpMethod.GET, "/jobs/" + jobId + "/metrics", null, "Failed to get job metrics")
        .map(HttpResponse::bodyAsJsonObject);
  }

  @Override
  public Single<String> stopJobWithSavepoint(
      String jobId, String savepointDirectory, boolean drain) {
    log.info("Stopping job {} with savepoint to: {}, drain: {}", jobId, savepointDirectory, drain);

    JsonObject requestBody =
        new JsonObject().put("targetDirectory", savepointDirectory).put("drain", drain);

    return executeRequest(
            HttpMethod.POST,
            "/jobs/" + jobId + "/stop",
            requestBody,
            "Failed to stop job with savepoint")
        .flatMap(
            response -> {
              String requestId = response.bodyAsJsonObject().getString(REQUEST_ID_FIELD);
              return pollSavepointStatus(jobId, requestId);
            })
        .doOnSuccess(
            savepointPath -> {
              log.info("Job {} stopped with savepoint at: {}", jobId, savepointPath);
            })
        .doOnError(
            error -> {
              log.error("Failed to stop job {} with savepoint", jobId, error);
            })
        .onErrorResumeNext(
            error -> {
              if (error instanceof FlinkSavepointException) {
                return Single.error(error);
              }
              return Single.error(
                  new FlinkSavepointException(jobId, "stop with savepoint", error.getMessage()));
            });
  }

  @Override
  public Completable rescaleJob(String jobId, int parallelism) {
    log.info("Rescaling job {} to parallelism: {}", jobId, parallelism);

    JsonObject requestBody = new JsonObject().put("parallelism", parallelism);

    return executeRequest(
            HttpMethod.PATCH, "/jobs/" + jobId + "/rescaling", requestBody, "Failed to rescale job")
        .doOnSuccess(
            response -> {
              log.info("Job {} rescaled successfully to parallelism: {}", jobId, parallelism);
            })
        .doOnError(
            error -> {
              log.error("Failed to rescale job {}", jobId, error);
            })
        .ignoreElement();
  }

  @Override
  public Completable deleteJar(String jarId) {
    log.info("Deleting JAR: {}", jarId);
    return executeRequest(HttpMethod.DELETE, "/jars/" + jarId, null, "Failed to delete JAR")
        .doOnSuccess(
            response -> {
              log.info("JAR {} deleted successfully", jarId);
            })
        .doOnError(
            error -> {
              log.error("Failed to delete JAR {}", jarId, error);
            })
        .ignoreElement();
  }

  @Override
  public Single<JsonArray> listJars() {
    log.debug("Listing uploaded JARs");
    return executeRequest(HttpMethod.GET, "/jars", null, "Failed to list JARs")
        .map(response -> response.bodyAsJsonObject().getJsonArray("files", new JsonArray()));
  }

  @Override
  public Single<JsonObject> getClusterConfiguration() {
    log.debug("Getting cluster configuration");
    return executeRequest(HttpMethod.GET, "/config", null, "Failed to get cluster configuration")
        .map(HttpResponse::bodyAsJsonObject);
  }

  /** Poll savepoint status until completion. */
  private Single<String> pollSavepointStatus(String jobId, String requestId) {
    return pollSavepointStatusWithRetry(jobId, requestId, 0);
  }

  private Single<String> pollSavepointStatusWithRetry(
      String jobId, String requestId, int retryCount) {
    if (retryCount >= flinkConfig.getSavepointMaxRetries()) {
      return Single.error(
          new FlinkSavepointException(
              String.format(
                  "Savepoint polling exceeded max retries (%d) for job %s",
                  flinkConfig.getSavepointMaxRetries(), jobId)));
    }

    return Single.defer(
        () ->
            executeRequest(
                    HttpMethod.GET,
                    "/jobs/" + jobId + "/savepoints/" + requestId,
                    null,
                    "Failed to get savepoint status")
                .flatMap(
                    response -> {
                      JsonObject savepointStatus = response.bodyAsJsonObject();
                      JsonObject status = savepointStatus.getJsonObject(STATUS_FIELD);
                      String state = status.getString("id");

                      if ("COMPLETED".equals(state)) {
                        JsonObject operation = savepointStatus.getJsonObject("operation");
                        return Single.just(operation.getString(LOCATION_FIELD));
                      } else if ("IN_PROGRESS".equals(state)) {
                        // Wait and retry
                        return Single.timer(
                                flinkConfig.getSavepointPollInterval(), TimeUnit.MILLISECONDS)
                            .flatMap(
                                tick ->
                                    pollSavepointStatusWithRetry(jobId, requestId, retryCount + 1));
                      } else {
                        // Failed state
                        String failureCause = status.getString("failure-cause", "Unknown error");
                        return Single.error(
                            new FlinkSavepointException(jobId, state, failureCause));
                      }
                    }));
  }

  /** Execute HTTP request to Flink REST API. */
  private Single<HttpResponse<Buffer>> executeRequest(
      HttpMethod method, String path, JsonObject body, String errorMessage) {

    HttpRequest<Buffer> request;

    // Prepare request based on HTTP method
    if (method == HttpMethod.GET) {
      request = webClient.prepareHttpGETRequest(flinkConfig.getHost(), flinkConfig.getPort(), path);
    } else if (method == HttpMethod.POST) {
      request =
          webClient.prepareHttpPOSTRequest(flinkConfig.getHost(), flinkConfig.getPort(), path);
    } else if (method == HttpMethod.PUT) {
      request = webClient.prepareHttpPUTRequest(flinkConfig.getHost(), flinkConfig.getPort(), path);
    } else {
      // For PATCH and DELETE, we'll use POST as fallback
      request =
          webClient.prepareHttpPOSTRequest(flinkConfig.getHost(), flinkConfig.getPort(), path);
    }

    request.timeout(flinkConfig.getRequestTimeout());

    if (body != null) {
      request.putHeader(CONTENT_TYPE, APPLICATION_JSON);
      return request
          .rxSendJsonObject(body)
          .flatMap(response -> validateResponse(response, errorMessage));
    } else {
      return request.rxSend().flatMap(response -> validateResponse(response, errorMessage));
    }
  }

  /** Validate HTTP response and handle errors. */
  private Single<HttpResponse<Buffer>> validateResponse(
      HttpResponse<Buffer> response, String errorMessage) {

    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      return Single.just(response);
    } else {
      int statusCode = response.statusCode();
      String responseBody = response.bodyAsString();

      log.error(
          "Flink API error: {}. Status: {}, Body: {}", errorMessage, statusCode, responseBody);

      // Return specific exceptions based on status code
      if (statusCode == 404) {
        return Single.error(new FlinkJobNotFoundException(extractJobIdFromError(responseBody)));
      } else if (statusCode >= 500) {
        return Single.error(
            new FlinkConnectionException(
                String.format("%s. Flink cluster may be unavailable", errorMessage)));
      } else {
        return Single.error(new FlinkApiException(errorMessage, statusCode, responseBody));
      }
    }
  }

  /** Extract job ID from error response if available. */
  private String extractJobIdFromError(String responseBody) {
    // Simple extraction, can be enhanced with JSON parsing if needed
    return "unknown";
  }
}
