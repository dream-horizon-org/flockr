package com.ascend.flockr.service.flink;

import com.ascend.flockr.config.AppConfig;
import com.ascend.flockr.dao.*;
import com.ascend.flockr.dao.taskdefinition.TaskWithJobInfo;
import com.ascend.flockr.exception.ErrorMessage;
import com.ascend.flockr.exception.RemoteServiceError;
import com.ascend.flockr.io.response.EventStreamTaskExecutionDetailResponse;
import com.ascend.flockr.io.response.TaskExecutionDetailResponse;
import com.ascend.flockr.model.task.JobExecutionLog;
import com.ascend.flockr.model.task.Task;
import com.ascend.flockr.model.task.TaskDefinition;
import com.ascend.flockr.model.task.constant.RuleType;
import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import com.ascend.flockr.model.task.rule.FlashRule;
import com.ascend.flockr.model.task.rule.JsonRule;
import com.ascend.flockr.model.task.rule.PatternSequenceRule;
import com.ascend.flockr.model.task.rule.Rule;
import com.ascend.flockr.service.FlinkClient;
import com.ascend.flockr.service.ResumeJobConfig;
import com.ascend.flockr.service.web.AsyncJobService;
import com.ascend.flockr.service.web.JobConfig;
import com.ascend.flockr.service.web.SlackService;
import com.ascend.flockr.util.MetricUtil;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.timgroup.statsd.StatsDClient;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;

@Slf4j
public class EventStreamJobService implements AsyncJobService<PatternSequenceRule> {

  private static final Map<FlinkJobState, TaskStatus> TERMINAL_STATE_AFTER_SUBMITTING =
      ImmutableMap.<FlinkJobState, TaskStatus>builder()
          .put(FlinkJobState.RUNNING, TaskStatus.Running)
          .put(FlinkJobState.FINISHED, TaskStatus.Completed)
          .put(FlinkJobState.FAILED, TaskStatus.Failed)
          .put(FlinkJobState.SUSPENDED, TaskStatus.Failed)
          .put(FlinkJobState.CANCELED, TaskStatus.Failed)
          .build();
  private static final Map<FlinkJobState, TaskStatus> TERMINAL_STATE_AFTER_PAUSING =
      ImmutableMap.<FlinkJobState, TaskStatus>builder()
          .put(FlinkJobState.RUNNING, TaskStatus.Running)
          .put(FlinkJobState.FINISHED, TaskStatus.Completed)
          .put(FlinkJobState.FAILED, TaskStatus.Failed)
          .put(FlinkJobState.SUSPENDED, TaskStatus.Failed)
          .put(FlinkJobState.CANCELED, TaskStatus.Paused)
          .build();
  private static final Map<FlinkJobState, TaskStatus> TERMINAL_STATE_AFTER_TERMINATING =
      ImmutableMap.<FlinkJobState, TaskStatus>builder()
          .put(FlinkJobState.RUNNING, TaskStatus.Running)
          .put(FlinkJobState.FINISHED, TaskStatus.Completed)
          .put(FlinkJobState.FAILED, TaskStatus.Failed)
          .put(FlinkJobState.SUSPENDED, TaskStatus.Failed)
          .put(FlinkJobState.CANCELED, TaskStatus.Terminated)
          .build();

  private final TaskType taskType = TaskType.eventStream;

  private final StatsDClient d11DDClient;
  private final FlinkClient flinkClient;
  private final TaskDefinitionReader taskReader;
  private final TaskDefinitionWriter taskWriter;
  private final JarDetailReader jarDetailReader;
  private final JobExecutionReader jobExecutionReader;
  private final JobExecutionWriter jobExecutionWriter;
  private final CohortReader cohortReader;
  private final SlackService slackService;
  private final Long periodicInterval;

  private final String OPERATION = "FLINK_OPERATION";
  private final String START_ASPECT = "FLINK_JOB_SUBMIT";
  private final String PAUSE_ASPECT = "FLINK_JOB_PAUSE";
  private final String RESUME_ASPECT = "FLINK_JOB_RESUME";
  private final String TERMINATE_ASPECT = "FLINK_JOB_TERMINATE";
  private final String MAINTENANCE_ASPECT = "FLINK_JOB_MAINTENANCE";

  private Map<RuleType, Class<?>> typeCaster =
      ImmutableMap.<RuleType, Class<?>>builder()
          .put(RuleType.tumblingWindow, JsonRule.class)
          .put(RuleType.streak, JsonRule.class)
          .put(RuleType.slidingWindow, JsonRule.class)
          .put(RuleType.globalCounter, JsonRule.class)
          .put(RuleType.triggerBatchQuery, FlashRule.class)
          .put(RuleType.sequence, PatternSequenceRule.class)
          .build();

  private final String JOB_FAILED_USER_SLACK_NOTIFY_FAILURE =
      "JOB_FAILED_USER_SLACK_NOTIFY_FAILURE";

  private final String COHORT_RULE_FAILED_USER_SLACK_NOTIFY =
      "Rule `%s` of cohort `%s` has failed.\n "
          + "In case you are not sure how to resolve this, please raise on #audience_engine_on_call";

  private final String RULE_FAILED_USER_SLACK_NOTIFY =
      "Rule `%s` has failed.\n "
          + "In case you are not sure how to resolve this, please raise on  #_d11_msd_oncall_giveaways";

  @Inject
  public EventStreamJobService(
      StatsDClient d11DDClient,
      FlinkClient flinkClient,
      TaskDefinitionReader taskReader,
      TaskDefinitionWriter taskWriter,
      JarDetailReader jarDetailReader,
      JobExecutionReader jobExecutionReader,
      JobExecutionWriter jobExecutionWriter,
      CohortReader cohortReader,
      SlackService slackService,
      AppConfig appConfig) {
    this.d11DDClient = d11DDClient;
    this.flinkClient = flinkClient;
    this.taskReader = taskReader;
    this.taskWriter = taskWriter;
    this.jarDetailReader = jarDetailReader;
    this.jobExecutionReader = jobExecutionReader;
    this.jobExecutionWriter = jobExecutionWriter;
    this.cohortReader = cohortReader;
    this.slackService = slackService;
    this.periodicInterval = appConfig.getTimer().getInterval().getMs();
  }

  @Override
  public Single<Boolean> isValid(Rule rule) {
    try {
      typeCaster.get(rule.getRuleType()).cast(rule);
    } catch (Exception exception) {
      log.error("error while parsing rule", exception);
      return Single.just(Boolean.FALSE);
    }

    return rule.validate().andThen(Single.defer(() -> Single.just(Boolean.TRUE)));
  }

  @Override
  public void startJob(Long taskId, JobConfig config) {
    String operation = "start_job";
    d11DDClient.increment(
        MetricUtil.aspect(OPERATION), "operation:" + operation, "type:method_call");
    d11DDClient.increment(MetricUtil.aspect(START_ASPECT));

    jarDetailReader
        .latestJar()
        .switchIfEmpty(Single.error(new Exception(ErrorMessage.NO_JAR_FOUND)))
        .map(jarId -> new JobExecutionLog(taskId, jarId))
        .doOnSuccess(jel -> log.info("jobExecutionLog: {}", jel))
        .flatMap(
            jel ->
                flinkClient
                    .submitJob(taskId, jel.getJarId())
                    .onErrorResumeNext(
                        exception -> {
                          log.error("Error submitting Flink job for taskId={}", taskId, exception);
                          if (exception instanceof TimeoutException) {
                            log.error(
                                "submit job API timed out, identifying jobId from active jobs");
                            return identifyActiveJobByTaskId(taskId)
                                .flatMap(
                                    jobDetail -> {
                                      if (jobDetail.isPresent()) {
                                        log.info("jobId successfully identified from active jobs");
                                        return Single.just(jobDetail.get().getJobId());
                                      } else {
                                        log.error("unable to identify jobId from active jobs");
                                        return Single.error(exception);
                                      }
                                    });
                          } else {
                            return Single.error(exception);
                          }
                        })
                    .map(
                        jobId -> {
                          jel.setId(jobId);
                          jel.setState(FlinkJobState.SUBMITTED.name());
                          return jel;
                        }))
        .doOnSuccess(jel -> log.info("jobExecutionLog: {}", jel))
        .doOnError(
            ignored ->
                taskWriter
                    .updateStatusAndUpdatedBy(
                        taskId, taskType, TaskStatus.Failed, config.getClientUser())
                    .doAfterSuccess(bool -> sendSlackNotificationToUserOnFailure(taskId))
                    .subscribe())
        .flatMap(
            jel ->
                jobExecutionWriter
                    .create(jel, taskType)
                    .doOnComplete(
                        () -> log.info("job execution log created for jobId: {}", jel.getId()))
                    .toSingle(() -> jel))
        .flatMap(
            jel ->
                taskWriter
                    .updateStatusAndUpdatedBy(
                        taskId, taskType, TaskStatus.Submitted, config.getClientUser())
                    .doOnSuccess(ignored -> log.info("task status updated for taskId: {}", taskId))
                    .map(ignored -> jel))
        .doOnSuccess(
            jobExecutionLog -> {
              String jobId = jobExecutionLog.getId();
              scheduleStateSync(
                  taskId, jobId, TERMINAL_STATE_AFTER_SUBMITTING, this::doNothing, START_ASPECT);
            })
        .doOnSubscribe(disposable -> log.info("startJob execution for taskId: {}", taskId))
        /* @see com.dream11.common.app.AbstractApplication#setDefaultRxSchedulers(Vertx) */
        .subscribeOn(Schedulers.computation())
        .subscribe(
            ignored -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION), "operation:" + operation, "type:processed");
              log.info("startJob execution success for taskId: {}", taskId);
            },
            err -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION), "operation:" + operation, "type:processing_failed");
              log.error("startJob execution failed for taskId: {}", taskId, err);
            });
  }

  @Override
  public void triggerJob(TaskDefinition task, JobConfig config) {
    startJob(task.getId(), config);
  }

  @Override
  public void pauseJob(Long taskId) {
    String operation = "pause_job";
    d11DDClient.increment(
        MetricUtil.aspect(OPERATION), "operation:" + operation, "type:method_call");
    d11DDClient.increment(MetricUtil.aspect(PAUSE_ASPECT));

    CompletableFuture<Optional<JobExecutionLog>> jelFuture = new CompletableFuture<>();

    jobExecutionReader
        .findLatestByTaskId(taskId)
        .doOnSuccess(jel -> jelFuture.complete(Optional.of(jel)))
        .doOnError(
            err -> {
              log.error("error while fetching latest jobExecutionLog for taskId: {}", taskId);
              jelFuture.complete(Optional.empty());
            })
        .doOnComplete(
            () -> {
              log.error("no jobExecutionLog exists for taskId: {}", taskId);
              jelFuture.complete(Optional.empty());
            })
        .filter(jel -> jel.getState().equals(FlinkJobState.RUNNING.name()))
        .doOnComplete(
            () -> {
              Optional<JobExecutionLog> jelOptional = jelFuture.get();
              if (jelOptional.isEmpty()) {
                return;
              }

              log.error("State in JobExecutionLog is not RUNNING for taskId: {}", taskId);
              String jobId = jelOptional.get().getId();
              syncState(
                  taskId,
                  jobId,
                  TERMINAL_STATE_AFTER_SUBMITTING,
                  this::getAndSetCheckpointPath,
                  PAUSE_ASPECT);
            })
        .flatMapCompletable(
            jel ->
                flinkClient
                    .terminateJob(jel.getId())
                    .doOnError(
                        err -> {
                          log.error("pauseJob execution failed for taskId: {}", taskId, err);
                          String jobId = jel.getId();
                          syncState(
                              taskId,
                              jobId,
                              TERMINAL_STATE_AFTER_PAUSING,
                              this::getAndSetCheckpointPath,
                              PAUSE_ASPECT);
                        })
                    .doOnComplete(
                        () -> {
                          String jobId = jel.getId();
                          scheduleStateSync(
                              taskId,
                              jobId,
                              TERMINAL_STATE_AFTER_PAUSING,
                              this::getAndSetCheckpointPath,
                              PAUSE_ASPECT);
                        }))
        .doOnSubscribe(disposable -> log.info("pauseJob execution for taskId: {}", taskId))
        /* @see com.dream11.common.app.AbstractApplication#setDefaultRxSchedulers(Vertx) */
        .subscribeOn(Schedulers.computation())
        .subscribe(
            () -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION), "operation:" + operation, "type:processed");
              log.info("pauseJob execution success for taskId: {}", taskId);
            },
            err -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION), "operation:" + operation, "type:processing_failed");
              log.error("pauseJob execution failed for taskId: {}", taskId, err);
              //                            DatadogUtils.sendErrorToDatadog(err);
            });
  }

  @Override
  public void resumeJob(Long taskId, ResumeJobConfig config) {
    String operation = "resume_job";
    d11DDClient.increment(
        MetricUtil.aspect(OPERATION), "operation:" + operation, "type:method_call");
    d11DDClient.increment(MetricUtil.aspect(RESUME_ASPECT));

    Single<String> jarId;
    if (config.isResumeUsingLatestArtifact()) {
      jarId =
          jarDetailReader
              .latestJar()
              .switchIfEmpty(Single.error(new Exception(ErrorMessage.NO_JAR_FOUND)));
    } else {
      jarId =
          jobExecutionReader
              .findLatestByTaskId(taskId)
              .map(JobExecutionLog::getJarId)
              .doOnSuccess(jarIdentifier -> log.info("previous jarId: {}", jarIdentifier))
              .doOnComplete(
                  () ->
                      log.info(("previous JobExecutionLog does not exist, fetching latest jar...")))
              .switchIfEmpty(
                  jarDetailReader
                      .latestJar()
                      .switchIfEmpty(Single.error(new Exception(ErrorMessage.NO_JAR_FOUND))));
    }

    Single<Optional<String>> savepoint;
    if (config.isResumeFromSavepoint()) {
      savepoint =
          jobExecutionReader
              .findLatestSavepoint(taskId)
              .doOnSuccess(
                  savepointPath ->
                      log.info("latest savepointPath: {} for taskId: {}", savepointPath, taskId))
              .doOnComplete(() -> log.info("no savepointPath exists for taskId: {}", taskId))
              .map(Optional::of)
              .switchIfEmpty(Single.just(Optional.empty()));
    } else {
      savepoint = Single.just(Optional.empty());
    }

    jarId
        .map(jarIdentifier -> new JobExecutionLog(taskId, jarIdentifier))
        .doOnSuccess(jel -> log.info("jobExecutionLog: {}", jel))
        .zipWith(
            savepoint,
            (jel, savepointPath) -> {
              Single<String> jobId;
              if (savepointPath.isPresent()) {
                jobId = flinkClient.submitJob(taskId, jel.getJarId(), savepointPath.get());
              } else {
                jobId = flinkClient.submitJob(taskId, jel.getJarId());
              }

              return jobId
                  .onErrorResumeNext(
                      exception -> {
                        log.error("Error submitting Flink job for taskId={}", taskId, exception);
                        if (exception instanceof TimeoutException) {
                          log.error("submit job API timed out, identifying jobId from active jobs");
                          return identifyActiveJobByTaskId(taskId)
                              .flatMap(
                                  jobDetail -> {
                                    if (jobDetail.isPresent()) {
                                      log.info("jobId successfully identified from active jobs");
                                      return Single.just(jobDetail.get().getJobId());
                                    } else {
                                      log.error("unable to identify jobId from active jobs");
                                      return Single.error(exception);
                                    }
                                  });
                        } else {
                          return Single.error(exception);
                        }
                      })
                  .map(
                      value -> {
                        jel.setId(value);
                        jel.setState(FlinkJobState.SUBMITTED.name());
                        return jel;
                      });
            })
        .flatMap(elementSingleSource -> elementSingleSource)
        .doOnSuccess(jel -> log.info("jobExecutionLog: {}", jel))
        .doOnError(
            ignored ->
                taskWriter
                    .updateStatusAndUpdatedBy(
                        taskId, taskType, TaskStatus.Failed, config.getClientUser())
                    .subscribe())
        .flatMap(
            jel ->
                jobExecutionWriter
                    .create(jel, taskType)
                    .doOnComplete(
                        () -> log.info("job execution log created for jobId: {}", jel.getId()))
                    .toSingle(() -> jel))
        .flatMap(
            jel ->
                taskWriter
                    .updateStatusAndUpdatedBy(
                        taskId, taskType, TaskStatus.Submitted, config.getClientUser())
                    .doOnSuccess(ignored -> log.info("task status updated for taskId: {}", taskId))
                    .map(ignored -> jel))
        .doOnSuccess(
            jobExecutionLog -> {
              String jobId = jobExecutionLog.getId();
              scheduleStateSync(
                  taskId, jobId, TERMINAL_STATE_AFTER_SUBMITTING, this::doNothing, RESUME_ASPECT);
            })
        .doOnSubscribe(disposable -> log.info("resumeJob execution for taskId: {}", taskId))
        /* @see com.dream11.common.app.AbstractApplication#setDefaultRxSchedulers(Vertx) */
        .subscribeOn(Schedulers.computation())
        .subscribe(
            ignored -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION), "operation:" + operation, "type:processed");
              log.info("resumeJob execution success for taskId: {}", taskId);
            },
            err -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION), "operation:" + operation, "type:processing_failed");
              log.error("resumeJob execution failed for taskId: {}", taskId, err);
            });
  }

  @Override
  public void terminateJob(Long taskId) {
    String operation = "terminate_job";
    d11DDClient.increment(
        MetricUtil.aspect(OPERATION), "operation:" + operation, "type:method_call");
    d11DDClient.increment(MetricUtil.aspect(TERMINATE_ASPECT));

    CompletableFuture<Optional<JobExecutionLog>> jelFuture = new CompletableFuture<>();

    jobExecutionReader
        .findLatestByTaskId(taskId)
        .doOnSuccess(jel -> jelFuture.complete(Optional.of(jel)))
        .doOnError(
            err -> {
              log.error("error while fetching latest jobExecutionLog for taskId: {}", taskId);
              jelFuture.complete(Optional.empty());
            })
        .doOnComplete(
            () -> {
              log.error("no jobExecutionLog exists for taskId: {}", taskId);
              jelFuture.complete(Optional.empty());
            })
        .filter(jel -> jel.getState().equals(FlinkJobState.RUNNING.name()))
        .doOnComplete(
            () -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION),
                  "operation:" + operation,
                  "type:processing_skipped");

              Optional<JobExecutionLog> jelOptional = jelFuture.get();
              if (jelOptional.isEmpty()) {
                return;
              }

              log.error("State in JobExecutionLog is not RUNNING for taskId: {}", taskId);
              String jobId = jelOptional.get().getId();
              syncState(
                  taskId,
                  jobId,
                  TERMINAL_STATE_AFTER_SUBMITTING,
                  this::getAndSetCheckpointPath,
                  TERMINATE_ASPECT);
            })
        .flatMapCompletable(
            jel ->
                flinkClient
                    .terminateJob(jel.getId())
                    .doOnComplete(
                        () ->
                            d11DDClient.increment(
                                MetricUtil.aspect(OPERATION),
                                "operation:" + operation,
                                "type:processed")))
        .doOnEvent(
            (ignored) -> {
              Optional<JobExecutionLog> jelOptional = jelFuture.get();
              if (jelOptional.isEmpty()) {
                return;
              }

              String jobId = jelOptional.get().getId();
              scheduleStateSync(
                  taskId,
                  jobId,
                  TERMINAL_STATE_AFTER_TERMINATING,
                  this::getAndSetCheckpointPath,
                  TERMINATE_ASPECT);
            })
        .doOnSubscribe(disposable -> log.info("terminateJob execution for taskId: {}", taskId))
        /* @see com.dream11.common.app.AbstractApplication#setDefaultRxSchedulers(Vertx) */
        .subscribeOn(Schedulers.computation())
        .subscribe(
            () -> log.info("terminateJob execution success for taskId: {}", taskId),
            err -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION), "operation:" + operation, "type:processing_failed");
              log.error("terminateJob execution failed for taskId: {}", taskId, err);
            });
  }

  @Override
  public Single<TaskExecutionDetailResponse<?>> taskExecutionDetail(Task task) {
    return Single.zip(
        Single.just(task),
        jobExecutionReader.findLatest10ByTaskId(task.getId()),
        jobExecutionReader
            .findAllSavepointByTaskId(task.getId())
            .map(EventStreamTaskExecutionDetailResponse::new),
        TaskExecutionDetailResponse::new);
  }

  @Override
  public void maintenance(Long taskId) {
    String operation = "task_maintenance";
    d11DDClient.increment(
        MetricUtil.aspect(OPERATION), "operation:" + operation, "type:method_call");
    d11DDClient.increment(MetricUtil.aspect(MAINTENANCE_ASPECT));
    jobExecutionReader
        .findLatestByTaskId(taskId)
        .flatMap(
            jel ->
                taskReader
                    .findStatusById(taskId)
                    .doOnSuccess(
                        taskStatus -> {
                          syncState(
                              taskId,
                              jel.getId(),
                              getJobStateTaskStatusMapping(taskStatus),
                              this::getAndSetCheckpointPath,
                              MAINTENANCE_ASPECT);
                        }))
        .doOnError(
            err -> log.error("error while fetching latest jobExecutionLog for taskId: {}", taskId))
        .doOnComplete(() -> log.error("no jobExecutionLog exists for taskId: {}", taskId))
        .doOnSubscribe(disposable -> log.info("maintenance execution for taskId: {}", taskId))
        /* @see com.dream11.common.app.AbstractApplication#setDefaultRxSchedulers(Vertx) */
        .subscribeOn(Schedulers.computation())
        .subscribe(
            (ignored) -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION), "operation:" + operation, "type:processed");
              log.info("maintenance execution success for taskId: {}", taskId);
            },
            err -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION), "operation:" + operation, "type:processing_failed");
              log.error("maintenance execution failed for taskId: {}", taskId, err);
            },
            () -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION),
                  "operation:" + operation,
                  "type:processing_skipped");
              log.info("maintenance execution completed for taskId: {}", taskId);
            });
  }

  @Override
  public void maintenance(List<TaskStatus> maintainableTaskStatus, Promise<Void> completePromise) {
    String operation = "job_maintenance";
    d11DDClient.increment(
        MetricUtil.aspect(OPERATION), "operation:" + operation, "type:method_call");

    Single.zip(
            taskReader
                .findAllWithLatestJobByStatusAndType(maintainableTaskStatus, taskType)
                .map(
                    taskWithJobInfoList ->
                        taskWithJobInfoList.stream()
                            .collect(
                                Collectors.toMap(
                                    TaskWithJobInfo::getJobId,
                                    taskWithJobInfo -> taskWithJobInfo))),
            flinkClient
                .jobsOverview()
                .map(
                    jobsOverview ->
                        jobsOverview.getJobs().stream()
                            .collect(
                                Collectors.toMap(
                                    JobDetailResponse::getJobId, jobDetail -> jobDetail))),
            (taskWithJobInfoMap, jobDetailMap) ->
                taskWithJobInfoMap.entrySet().stream()
                    .collect(
                        Collectors.toMap(
                            Map.Entry::getKey,
                            entry ->
                                new ImmutablePair<TaskWithJobInfo, JobDetailResponse>(
                                    entry.getValue(), jobDetailMap.get(entry.getKey())) {})))
        .map(Map::values)
        .flatMapObservable(Observable::fromIterable)
        .filter(
            pair -> {
              if (pair.getRight() == null) {
                return true;
              }
              return
              // if job state is not in sync with remote cluster
              !pair.getLeft().getJobStatus().equals(pair.getRight().getState().name())
                  || // if task status is not in sync with stable job state
                  (TERMINAL_STATE_AFTER_SUBMITTING.containsKey(pair.getRight().getState())
                      && TERMINAL_STATE_AFTER_SUBMITTING.get(pair.getRight().getState())
                          != pair.getLeft().getTaskStatus());
            })
        .map(
            pair -> {
              if (pair.getRight() == null) {
                return jobExecutionWriter
                    .updateState(
                        pair.getLeft().getJobId(),
                        pair.getLeft().getId(),
                        taskType,
                        FlinkJobState.FAILED.name())
                    .doOnError(
                        err -> {
                          log.error(
                              "Failed to update job state for taskId: {}",
                              pair.getLeft().getId(),
                              err);
                        })
                    .flatMap(
                        (ignored) ->
                            taskWriter
                                .updateStatusByIdAndUpdatedAt(
                                    pair.getLeft().getId(),
                                    taskType,
                                    TaskStatus.Failed,
                                    pair.getLeft().getTaskUpdatedAt())
                                .doOnError(
                                    err -> {
                                      log.error(
                                          "Failed to update task status for taskId: {}",
                                          pair.getLeft().getId(),
                                          err);
                                    }))
                    .ignoreElement();
              }
              return syncState(
                  pair.getLeft().getId(),
                  pair.getLeft().getTaskUpdatedAt(),
                  pair.getRight(),
                  pair.getLeft().getJobUpdatedAt(),
                  getJobStateTaskStatusMapping(pair.getLeft().getTaskStatus()),
                  this::getAndSetCheckpointPath,
                  MAINTENANCE_ASPECT);
            })
        .doOnSubscribe(disposable -> log.info("flink jobs maintenance starting..."))
        .subscribe(
            completable ->
                completable.subscribe(
                    () -> {},
                    err -> {
                      log.error("Error submitting Flink job ");
                    }),
            err -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION), "operation:" + operation, "type:processing_failed");
              log.error("flink jobs maintenance execution failed", err);
              completePromise.complete();
            },
            () -> {
              d11DDClient.increment(
                  MetricUtil.aspect(OPERATION), "operation:" + operation, "type:processed");
              log.info("flink jobs maintenance execution succeeded");
              completePromise.complete();
            });
  }

  @Override
  public Maybe<Long> findTaskIdByJobRefId(String jobId) {
    return jobExecutionReader.findById(jobId).map(JobExecutionLog::getTaskId);
  }

  public Completable updateJelSyncedTrueAndWatermarkByJobId(Map<String, Long> jobWatermarks) {
    return jobExecutionWriter
        .updatedMultipleSyncedTrueAndWatermarkById(jobWatermarks)
        .doOnComplete(() -> log.info("Update synced true and job watermark on merge write success"))
        .doOnError(
            err ->
                log.error(
                    "Update synced true and job watermark on merge write success Failed due to: ",
                    err));
  }

  private void syncState(
      Long taskId,
      String jobId,
      Map<FlinkJobState, TaskStatus> expectedJobState,
      BiConsumer<Long, String> onJobFailure,
      String callerAspect) {
    flinkClient
        .jobDetail(jobId)
        .doOnSuccess(job -> log.info("jobDetailResponse: {}", job))
        .doOnError(
            throwable -> {
              if (throwable instanceof RemoteServiceError
                  && ((RemoteServiceError) throwable).getResponse().statusCode() == 404) {
                sendSlackNotificationToUserOnFailure(taskId);

                jobExecutionWriter
                    .updateState(jobId, taskId, taskType, FlinkJobState.FAILED.name())
                    .doOnError(
                        err -> {
                          log.error("Failed to update job state for taskId: {}", taskId, err);
                        })
                    .flatMap(
                        (ignored) ->
                            taskWriter
                                .updateStatus(taskId, taskType, TaskStatus.Failed)
                                .doOnError(
                                    err -> {
                                      log.error(
                                          "Failed to update task status for taskId: {}",
                                          taskId,
                                          err);
                                    }))
                    .subscribe();
              }
            })
        .doAfterSuccess(
            jobDetailResponse -> {
              if (jobDetailResponse.getState().equals(FlinkJobState.FAILED)) {
                sendSlackNotificationToUserOnFailure(taskId);
              }
            })
        .flatMapCompletable(
            job -> syncState(taskId, job, expectedJobState, onJobFailure, callerAspect))
        .doOnSubscribe(disposable -> log.info("syncState timer. jobId: {}", jobId))
        .subscribe(
            () -> log.info("syncState execution success for taskId: {}", taskId),
            err -> {
              log.error("syncState execution failed for taskId: {}", taskId, err);
            });
  }

  private Completable syncState(
      Long taskId,
      JobDetailResponse job,
      Map<FlinkJobState, TaskStatus> expectedJobState,
      BiConsumer<Long, String> onJobFailure,
      String callerAspect) {
    return jobExecutionWriter
        .update(
            job.getJobId(),
            taskId,
            taskType,
            job.getState().name(),
            job.getStartTime(),
            job.getEndTime())
        .doOnSuccess(ignored -> log.info("job state updated for jobId: {}", job.getJobId()))
        .filter(ignored -> expectedJobState.containsKey(job.getState()))
        .doOnComplete(
            () ->
                scheduleStateSync(
                    taskId, job.getJobId(), expectedJobState, onJobFailure, callerAspect))
        .flatMapCompletable(
            ignored1 -> {
              TaskStatus taskStatus = expectedJobState.get(job.getState());
              d11DDClient.increment(MetricUtil.aspect(callerAspect, taskStatus.name()));
              return taskWriter
                  .updateStatus(taskId, taskType, taskStatus)
                  .filter(Boolean::booleanValue)
                  .doOnSuccess(
                      ignored -> {
                        log.info("task status updated for taskId: {}", taskId);
                        if (job.getState() == FlinkJobState.SUSPENDED
                            || job.getState() == FlinkJobState.FAILED
                            || job.getState() == FlinkJobState.CANCELED) {
                          onJobFailure.accept(taskId, job.getJobId());
                        }
                      })
                  .ignoreElement();
            });
  }

  private Completable syncState(
      Long taskId,
      Long taskSnapShotTimeInSec,
      JobDetailResponse job,
      Long jobSnapShotTimeInSec,
      Map<FlinkJobState, TaskStatus> expectedJobState,
      BiConsumer<Long, String> onJobFailure,
      String callerAspect) {
    return jobExecutionWriter
        .updateByUpdatedAt(
            job.getJobId(),
            taskId,
            taskType,
            job.getState().name(),
            job.getStartTime(),
            job.getEndTime(),
            jobSnapShotTimeInSec)
        .doOnComplete(() -> log.info("job state updated for jobId: {}", job.getJobId()))
        .toSingleDefault(Boolean.TRUE)
        .filter(ignored -> expectedJobState.containsKey(job.getState()))
        .doOnComplete(
            () ->
                scheduleStateSync(
                    taskId, job.getJobId(), expectedJobState, onJobFailure, callerAspect))
        .flatMapCompletable(
            ignored1 -> {
              TaskStatus taskStatus = expectedJobState.get(job.getState());
              d11DDClient.increment(MetricUtil.aspect(callerAspect, taskStatus.name()));
              return taskWriter
                  .updateStatusByIdAndUpdatedAt(taskId, taskType, taskStatus, taskSnapShotTimeInSec)
                  .filter(Boolean::booleanValue)
                  .doOnSuccess(
                      ignored -> {
                        log.info("task status updated for taskId: {}", taskId);
                        if (job.getState() == FlinkJobState.SUSPENDED
                            || job.getState() == FlinkJobState.FAILED
                            || job.getState() == FlinkJobState.CANCELED) {
                          onJobFailure.accept(taskId, job.getJobId());
                        }
                      })
                  .ignoreElement();
            });
  }

  private void updateSavepoint(Long taskId, String jobId, String triggerId, Action onComplete) {
    flinkClient
        .savepointStatus(jobId, triggerId)
        .filter(
            savepointStatus ->
                savepointStatus.getStatus() != null
                    && savepointStatus.getStatus().getId().equals("COMPLETED"))
        .doOnSuccess(ignored -> onComplete.run())
        .doOnError(ignored -> onComplete.run())
        .doOnComplete(() -> scheduleSavepointUpdate(taskId, jobId, triggerId, onComplete))
        .flatMapCompletable(
            savepointStatus -> {
              SavepointStatusResponse.Operation operation = savepointStatus.getOperation();
              if (operation != null && operation.getLocation() != null) {
                return jobExecutionWriter
                    .addSavepoint(taskId, jobId, savepointStatus.getOperation().getLocation())
                    .doOnComplete(() -> log.info("s3Path updated for taskId: {}", taskId));
              } else {
                log.error(
                    "error in savepoint operation for taskId: {}. error {}",
                    taskId,
                    operation != null ? operation.getCause() : null);
                return Completable.complete();
              }
            })
        .doOnSubscribe(disposable -> log.info("updateSavepoint timer. jobId: {}", jobId))
        .subscribe(
            () -> log.info("updateSavepoint execution success for taskId: {}", taskId),
            err -> {
              log.error("updateSavepoint execution failed for taskId: {}", taskId, err);
            });
  }

  private Single<Optional<JobDetailResponse>> identifyActiveJobByTaskId(Long taskId) {
    return Single.zip(
        taskWriter.findDetailById(taskId).toSingle(),
        flinkClient.jobsOverview(),
        (taskDefinition, jobsOverview) -> {
          String expectedJobName =
              taskDefinition.getRule().getJobName() + ":" + taskDefinition.getId();
          return jobsOverview.getJobs().stream()
              .filter(jobDetailResponse -> jobDetailResponse.getName().equals(expectedJobName))
              .filter(jobDetailResponse -> jobDetailResponse.getEndTime() < 0)
              .findAny();
        });
  }

  private void scheduleStateSync(
      Long taskId,
      String jobId,
      Map<FlinkJobState, TaskStatus> expectedJobState,
      BiConsumer<Long, String> onJobFailure,
      String callerAspect) {
    Vertx.currentContext()
        .owner()
        .setTimer(
            periodicInterval,
            timer -> syncState(taskId, jobId, expectedJobState, onJobFailure, callerAspect));
    log.info("assertStableJobState timer set. jobId: {}", jobId);
  }

  private void scheduleSavepointUpdate(
      Long taskId, String jobId, String triggerId, Action onComplete) {
    Vertx.currentContext()
        .owner()
        .setTimer(periodicInterval, timer -> updateSavepoint(taskId, jobId, triggerId, onComplete));
    log.info("updateSavepoint timer set. jobId: {}", jobId);
  }

  private void getAndSetCheckpointPath(Long taskId, String jobId) {
    flinkClient
        .checkpointStatistics(jobId)
        .filter(
            checkpointStatistics ->
                checkpointStatistics.getLatest().getCompleted() != null
                    && checkpointStatistics
                        .getLatest()
                        .getCompleted()
                        .getStatus()
                        .equalsIgnoreCase("COMPLETED")
                    && !checkpointStatistics.getLatest().getCompleted().isDiscarded())
        .doOnComplete(() -> log.info("no checkpoint to update for jobId: {}", jobId))
        .flatMapCompletable(
            checkpointStatistics ->
                jobExecutionWriter
                    .addSavepoint(
                        taskId, jobId, checkpointStatistics.getLatest().getCompleted().getS3Path())
                    .doOnComplete(
                        () ->
                            log.info(
                                "checkpoint info saved for taskId: {}, jobId: {}", taskId, jobId)))
        .doOnSubscribe(
            disposable -> log.info("fetching checkpoint statistics for jobId: {}", jobId))
        .subscribe(
            () -> {},
            ex -> {
              log.error("error while fetching checkpoint for jobId: {}", jobId, ex);
            });
  }

  private void sendSlackNotificationToUserOnFailure(Long taskId) {
    Maybe<TaskDefinition> taskDefinitionMaybe = taskReader.findDetailById(taskId);
    taskDefinitionMaybe
        .doOnSuccess(
            taskDefinition ->
                sendSlackNotificationToUserOnFailure(
                    taskDefinition.getName(),
                    taskDefinition.getCreatedBy(),
                    taskDefinition.getCohortId()))
        .subscribe();
  }

  private void sendSlackNotificationToUserOnFailure(
      String ruleName, String userEmail, Long cohortId) {
    if (!validUser(userEmail)) {
      return;
    }

    String messageHeader = ":alert-gif: Rule Execution Failure";
    Completable sendSlackNotificationCompletable;
    if (cohortId != null) {
      sendSlackNotificationCompletable =
          cohortReader
              .findCohortAndOwnerById(cohortId)
              .flatMapCompletable(
                  cohort -> {
                    String cohortName = cohort.getName();
                    String cohortRuleFailureMessage =
                        String.format(COHORT_RULE_FAILED_USER_SLACK_NOTIFY, ruleName, cohortName);

                    return Observable.fromIterable(cohort.getOwner())
                        .flatMapCompletable(
                            ownerEmail ->
                                slackService.postMessageWithHeaderRed(
                                    ownerEmail, cohortRuleFailureMessage, cohortId, messageHeader))
                        .onErrorResumeNext(
                            err -> {
                              d11DDClient.increment(
                                  MetricUtil.aspect(JOB_FAILED_USER_SLACK_NOTIFY_FAILURE),
                                  "type:" + taskType);
                              log.error(
                                  "error while sending slack notification to cohort owner on Job Failure: ",
                                  err);
                              return Completable.complete();
                            });
                  });
    } else {
      String message = String.format(RULE_FAILED_USER_SLACK_NOTIFY, ruleName);
      sendSlackNotificationCompletable =
          slackService.postMessageWithHeaderRed(userEmail, message, null, messageHeader);
    }
    sendSlackNotificationCompletable.subscribe(
        () -> log.info("Slack notification sent to users on Job Failure"),
        err -> {
          d11DDClient.increment(
              MetricUtil.aspect(JOB_FAILED_USER_SLACK_NOTIFY_FAILURE), "type:" + taskType);
          log.error("error while sending slack notification to users on Job Failure: ", err);
        });
  }

  private void doNothing(Long taskId, String jobId) {}

  private Map<FlinkJobState, TaskStatus> getJobStateTaskStatusMapping(TaskStatus taskStatus) {
    if (taskStatus == TaskStatus.Submitted) return TERMINAL_STATE_AFTER_SUBMITTING;
    else if (taskStatus == TaskStatus.Pausing) return TERMINAL_STATE_AFTER_PAUSING;
    else if (taskStatus == TaskStatus.Terminating) return TERMINAL_STATE_AFTER_TERMINATING;
    else return TERMINAL_STATE_AFTER_SUBMITTING;
  }
}
