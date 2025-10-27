package com.ascend.flockr.service.flink;

import com.ascend.flockr.config.AppConfig;
import com.ascend.flockr.service.FlinkClient;
import com.ascend.flockr.service.WebHttpClient;
import com.google.inject.Inject;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FlinkClientImpl implements FlinkClient {

  private static final String JOB_DETAIL_URI = "/jobs/%s";
  private static final String JOB_OVERVIEW_URI = "/jobs/overview";
  private static final String CHECKPOINT_STATISTICS = "/jobs/%s/checkpoints";
  private static final String SUBMIT_JOB_URI = "/jars/%s/run";
  private static final String SUBMIT_JOB_PARAM_ARGS = "--taskId,%s,--opPar,1";
  private static final String TRIGGER_SAVEPOINT_URI = "/jobs/%s/savepoints";
  private static final String SAVEPOINT_STATUS_URI = "/jobs/%s/savepoints/%s";
  private static final String TERMINATE_JOB_URI = "/jobs/%s";
  private final String host;
  private final Integer port;
  private final Long timeout;
  private final String directory;
  private final WebHttpClient httpClient;

  @Inject
  public FlinkClientImpl(AppConfig appConfig, WebHttpClient httpClient) {
    this.host = appConfig.getFlink().getHost();
    this.port = appConfig.getFlink().getPort();
    this.timeout = appConfig.getFlink().getTimeout().getMs();
    this.directory = appConfig.getFlink().getDirectory();
    this.httpClient = httpClient;
  }

  @Override
  public Single<JobDetailResponse> jobDetail(String jobId) {
    String uri = String.format(JOB_DETAIL_URI, jobId);
    return httpClient
        .get(timeout, host, port, uri, JobDetailResponse.class)
        .retry(1, this::timedOut);
  }

  @Override
  public Single<JobsOverviewResponse> jobsOverview() {
    return httpClient
        .get(timeout, host, port, JOB_OVERVIEW_URI, JobsOverviewResponse.class)
        .retry(1, this::timedOut);
  }

  @Override
  public Single<CheckpointStatistics> checkpointStatistics(String jobId) {
    String uri = String.format(CHECKPOINT_STATISTICS, jobId);
    return httpClient
        .get(timeout, host, port, uri, CheckpointStatistics.class)
        .retry(1, this::timedOut);
  }

  @Override
  public Single<String> submitJob(Long taskId, String jarId) {
    String uri = String.format(SUBMIT_JOB_URI, jarId);
    Map<String, String> queryParam =
        Map.of("programArg", String.format(SUBMIT_JOB_PARAM_ARGS, taskId));
    return httpClient
        .post(timeout, host, port, uri, queryParam, SubmitJobResponse.class)
        .map(SubmitJobResponse::getJobId);
  }

  @Override
  public Single<String> submitJob(Long taskId, String jarId, String savepointPath) {
    String uri = String.format(SUBMIT_JOB_URI, jarId);
    Map<String, String> queryParam =
        Map.of(
            "savepointPath",
            savepointPath,
            "programArg",
            String.format(SUBMIT_JOB_PARAM_ARGS, taskId));
    return httpClient
        .post(timeout, host, port, uri, queryParam, SubmitJobResponse.class)
        .map(SubmitJobResponse::getJobId);
  }

  @Override
  public Single<String> triggerSavepointAndCancelJob(String jobId) {
    String uri = String.format(TRIGGER_SAVEPOINT_URI, jobId);
    TriggerSavepointRequest request = new TriggerSavepointRequest(true, directory);
    return httpClient
        .post(timeout, host, port, uri, request, TriggerSavepointResponse.class)
        .map(TriggerSavepointResponse::getTriggerId);
  }

  @Override
  public Single<SavepointStatusResponse> savepointStatus(String jobId, String triggerId) {
    String uri = String.format(SAVEPOINT_STATUS_URI, jobId, triggerId);
    return httpClient
        .get(timeout, host, port, uri, SavepointStatusResponse.class)
        .retry(1, this::timedOut);
  }

  @Override
  public Completable terminateJob(String jobId) {
    return httpClient.patch(timeout, host, port, String.format(TERMINATE_JOB_URI, jobId), 202);
  }

  private boolean timedOut(Throwable exception) {
    if (exception instanceof TimeoutException) {
      log.error("http request timed out, retrying...");
      return true;
    } else {
      return false;
    }
  }
}
