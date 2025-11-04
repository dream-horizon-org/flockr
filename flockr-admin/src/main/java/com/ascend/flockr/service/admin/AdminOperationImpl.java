package com.ascend.flockr.service.admin;

import com.ascend.flockr.dao.TaskDefinitionReader;
import com.ascend.flockr.dao.TaskDefinitionWriter;
import com.ascend.flockr.dao.taskdefinition.TaskSchedule;
import com.ascend.flockr.dao.taskdefinition.TaskWithCohortInfo;
import com.ascend.flockr.exception.DefinedException;
import com.ascend.flockr.exception.ErrorEntity;
import com.ascend.flockr.io.request.UpdateTaskRequest;
import com.ascend.flockr.io.response.TaskExecutionDetailResponse;
import com.ascend.flockr.io.response.TaskInfoVerbose;
import com.ascend.flockr.model.AuditLog.AuditLog;
import com.ascend.flockr.model.AuditLog.AuditLogAction;
import com.ascend.flockr.model.AuditLog.AuditLogValue;
import com.ascend.flockr.model.crontrigger.CronTrigger;
import com.ascend.flockr.model.crontrigger.CronTriggerStatus;
import com.ascend.flockr.model.task.Task;
import com.ascend.flockr.model.task.TaskDefinition;
import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import com.ascend.flockr.service.AdminOperation;
import com.ascend.flockr.service.AuditLogService;
import com.ascend.flockr.service.ResumeJobConfig;
import com.ascend.flockr.service.web.AsyncJobService;
import com.ascend.flockr.service.web.JobConfig;
import com.ascend.flockr.util.CronTimeZone;
import com.ascend.flockr.util.CronUtil;
import com.ascend.flockr.util.ListTypeTransformer;
import com.ascend.flockr.util.Utility;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class AdminOperationImpl implements AdminOperation {

  private final TaskDefinitionReader taskDefinitionReader;
  private final TaskDefinitionWriter taskDefinitionWriter;
  private final TaskInfoVerboseMapper taskInfoVerboseMapper = new TaskInfoVerboseMapper();
  private final ListTypeTransformer<TaskDefinition, TaskInfoVerbose> taskDefinitionListTransformer =
      new ListTypeTransformer<>();
  private final Map<TaskType, AsyncJobService<?>> jobServiceKeeper;
  private final AuditLogService auditLogService;

  Map<TaskType, Set<TaskStatus>> triggerableTaskStatus =
      ImmutableMap.of(
          TaskType.eventStream,
          ImmutableSet.of(TaskStatus.Created, TaskStatus.Failed, TaskStatus.Completed),
          TaskType.storedData,
          ImmutableSet.of(
              TaskStatus.Created, TaskStatus.Running, TaskStatus.Failed, TaskStatus.Completed));

  Map<TaskType, Set<TaskStatus>> resumableTaskStatus =
      ImmutableMap.of(
          TaskType.eventStream,
          ImmutableSet.of(TaskStatus.Paused, TaskStatus.Failed),
          TaskType.storedData,
          ImmutableSet.of(TaskStatus.Paused, TaskStatus.Failed));

  Set<TaskStatus> terminableTaskStatus =
      ImmutableSet.of(
          TaskStatus.Scheduled,
          TaskStatus.Running,
          TaskStatus.Paused,
          TaskStatus.Failed,
          TaskStatus.Completed);

  public AdminOperationImpl(
      TaskDefinitionReader taskDefinitionReader,
      TaskDefinitionWriter taskDefinitionWriter,
      Map<TaskType, AsyncJobService<?>> jobServiceKeeper,
      AuditLogService auditLogService) {
    this.taskDefinitionReader = taskDefinitionReader;
    this.taskDefinitionWriter = taskDefinitionWriter;
    this.jobServiceKeeper = jobServiceKeeper;
    this.auditLogService = auditLogService;
  }

  public Maybe<TaskInfoVerbose> findTaskInfoVerboseById(Long taskId) {
    return taskDefinitionReader.findDetailById(taskId).map(taskInfoVerboseMapper);
  }

  @Override
  public Single<List<TaskInfoVerbose>> findAllTaskByCohortId(Long cohortId) {
    return taskDefinitionReader
        .findAllByCohortId(cohortId)
        .zipWith(Single.just(taskInfoVerboseMapper), taskDefinitionListTransformer);
  }

  @Override
  public Single<TaskExecutionDetailResponse<?>> taskExecutionDetail(Long taskId) {
    return taskDefinitionReader
        .findById(taskId)
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.NO_SUCH_TASK)))
        .flatMap(task -> jobServiceKeeper.get(task.getType()).taskExecutionDetail(task));
  }

  private boolean isTriggerable(Task task) {
    boolean triggerableStatus =
        triggerableTaskStatus.get(task.getType()).contains(task.getStatus());
    boolean additionalCheckPassed;
    if (task.getType() == TaskType.storedData && task.getCronExpression() == null) {
      additionalCheckPassed = task.getStatus() != TaskStatus.Running;
    } else {
      additionalCheckPassed = true;
    }
    return triggerableStatus && additionalCheckPassed;
  }

  @Override
  public Completable trigger(Long taskId, String client, String clientUser) {
    return taskDefinitionWriter
        .findDetailById(taskId)
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.NO_SUCH_TASK)))
        .filter(this::isTriggerable)
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.OPERATION_NOT_SUPPORTED)))
        .doOnSuccess(
            taskDefinition -> {
              TaskType type = taskDefinition.getType();
              if (taskDefinition.getCronExpression() != null
                  && taskDefinition.getStatus() != TaskStatus.Running) {
                // for rules created without start time
                jobServiceKeeper
                    .get(type)
                    .startJob(taskDefinition.getId(), new JobConfig(clientUser));
              } else {
                jobServiceKeeper.get(type).triggerJob(taskDefinition, new JobConfig(clientUser));
              }
            })
        .ignoreElement();
  }

  @Override
  public Completable pause(Long taskId, String client, String clientUser) {
    return taskDefinitionReader
        .findById(taskId)
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.NO_SUCH_TASK)))
        .filter(
            task ->
                task.getType() == TaskType.eventStream
                    || (task.getType() == TaskType.storedData && task.getCronExpression() != null))
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.OPERATION_NOT_SUPPORTED)))
        .filter(task -> task.getStatus() == TaskStatus.Running)
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.TASK_NOT_RUNNING)))
        .flatMap(
            task ->
                taskDefinitionWriter
                    .updateStatusAndUpdatedBy(
                        taskId, task.getType(), TaskStatus.Pausing, clientUser)
                    .doOnSuccess(tas -> jobServiceKeeper.get(task.getType()).pauseJob(taskId)))
        .ignoreElement();
  }

  @Override
  public Completable resume(Long taskId, String client, String clientUser) {
    return taskDefinitionReader
        .findDetailById(taskId)
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.NO_SUCH_TASK)))
        .filter(
            taskDefinition ->
                resumableTaskStatus
                    .get(taskDefinition.getType())
                    .contains(taskDefinition.getStatus()))
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.TASK_NOT_RESUMABLE)))
        .doOnSuccess(
            taskDefinition -> {
              TaskType type = taskDefinition.getType();
              jobServiceKeeper
                  .get(type)
                  .resumeJob(
                      taskDefinition.getId(),
                      new ResumeJobConfig(
                          clientUser,
                          Utility.getOrDefault(
                              taskDefinition.getResumeUsingLatestArtifact(), false),
                          Utility.getOrDefault(taskDefinition.getResumeFromSavepoint(), true),
                          taskDefinition.getStatus()));
            })
        .ignoreElement();
  }

  private Single<Boolean> terminateTask(Task task, String clientUser) {

    if (task.getStatus() == TaskStatus.Running) {
      return taskDefinitionWriter
          .updateEndDateAndStatus(
              task.getId(),
              task.getType(),
              TaskStatus.Terminating,
              clientUser,
              System.currentTimeMillis() / 1000)
          .doOnSuccess(ignored -> jobServiceKeeper.get(task.getType()).terminateJob(task.getId()));
    } else {
      return taskDefinitionWriter.updateEndDateAndStatus(
          task.getId(),
          task.getType(),
          TaskStatus.Terminated,
          clientUser,
          System.currentTimeMillis() / 1000);
    }
  }

  @Override
  public Completable terminate(Long taskId, String client, String clientUser) {
    return taskDefinitionReader
        .findById(taskId)
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.NO_SUCH_TASK)))
        .filter(task -> terminableTaskStatus.contains(task.getStatus()))
        .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.TASK_NOT_TERMINABLE)))
        .flatMap(
            task -> {
              if (task.getCohortId() != null) {
                return taskDefinitionReader
                    .findIfCohortOwnerExists(task.getCohortId(), clientUser)
                    .filter(isOwner -> isOwner)
                    .switchIfEmpty(
                        Single.error(
                            new DefinedException(ErrorEntity.USER_NOT_AUTHORIZED_TO_UPDATE_COHORT)))
                    .map(ignored -> task);
              } else return Single.just(task);
            })
        .flatMap(
            task ->
                terminateTask(task, clientUser)
                    .doOnSuccess(
                        ignored -> {
                          if (task.getCohortId() != null) {
                            auditLogService.create(
                                AuditLog.builder()
                                    .cohortId(task.getCohortId())
                                    .taskId(taskId)
                                    .action(AuditLogAction.RULE_TERMINATED)
                                    .value(new AuditLogValue(null, task.getName()))
                                    .createdBy(clientUser)
                                    .build());
                          }
                        }))
        .ignoreElement();
  }

    private void validateStartDate(Long startDate, Long endDate, Long taskId) {
        if (startDate > endDate) {
            throw new DefinedException(ErrorEntity.INVALID_START_DATE, taskId);
        }
    }

    private void validateEndDate(Long endDate, Long startDate, Long expirationDate, Long taskId) {
        if (endDate < startDate) {
            throw new DefinedException(ErrorEntity.INVALID_END_DATE, taskId);
        }

        if (endDate > expirationDate) {
            throw new DefinedException(ErrorEntity.INVALID_END_DATE, taskId);
        }
    }

    private void validateCron(String cronExpression) {
        if (cronExpression == null) {
            throw new DefinedException(ErrorEntity.CRON_NOT_FOUND);
        }
    }

    private Single<TaskSchedule> validateAndBuildTaskSchedule(
            UpdateTaskRequest request, TaskWithCohortInfo task) {

        if (request.getStartDate() != null) {
            validateStartDate(
                    request.getStartDate(),
                    request.getEndDate() != null ? request.getEndDate() : task.getEndTime(),
                    task.getId());
        }

        if (request.getEndDate() != null) {
            validateEndDate(
                    request.getEndDate(),
                    request.getStartDate() != null ? request.getStartDate() : task.getStartTime(),
                    task.getCohortExpiry(),
                    task.getId());
        }

        if (request.getCronExpression() != null) {
            validateCron(task.getCronExpression());
        }

        return Single.just(
                new TaskSchedule(
                        request.getCronExpression(), request.getStartDate(), request.getEndDate()));
    }

    @Override
    @SneakyThrows
    public Completable updateTask(
            Long taskId, UpdateTaskRequest request, String client, String clientUser) {
        return taskDefinitionReader
                .findTaskWithCohortInfoById(taskId)
                .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.NO_SUCH_TASK)))
                .filter(task -> task.getCohortId() != null)
                .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.COHORT_NOT_FOUND)))
                .filter(task -> !task.getExpired())
                .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.COHORT_EXPIRED)))
                .flatMap(
                        task ->
                                taskDefinitionReader
                                        .findIfCohortOwnerExists(task.getCohortId(), clientUser)
                                        .filter(isOwner -> isOwner)
                                        .switchIfEmpty(
                                                Single.error(
                                                        new DefinedException(ErrorEntity.USER_NOT_AUTHORIZED_TO_UPDATE_COHORT)))
                                        .map(ignored -> task))
                .filter(
                        task ->
                                (task.getStatus() == TaskStatus.Scheduled)
                                        && ((task.getStartTime() * 1000) - System.currentTimeMillis() > 60000)
                                        || (task.getStatus() == TaskStatus.Running)
                                        && ((task.getEndTime() * 1000) - System.currentTimeMillis() > 60000))
                .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.OPERATION_NOT_SUPPORTED)))
                .filter(task -> !(request.getStartDate() != null && task.getStatus() == TaskStatus.Running))
                .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.OPERATION_NOT_SUPPORTED)))
                .flatMap(
                        task ->
                                validateAndBuildTaskSchedule(request, task)
                                        .flatMap(
                                                taskSchedule -> {
                                                    if (request.getCronExpression() != null
                                                            && task.getStatus() == TaskStatus.Running) {
                                                        CronTrigger cronTrigger =
                                                                CronTrigger.builder()
                                                                        .taskId(taskId)
                                                                        .requestId(UUID.randomUUID().toString())
                                                                        .status(CronTriggerStatus.ACTIVE)
                                                                        .cronExpression(request.getCronExpression())
                                                                        .nextExecutionTime(
                                                                                CronUtil.nextExecutionTimeByZoneId(
                                                                                        request.getCronExpression(),
                                                                                        Instant.now().getEpochSecond(),
                                                                                        CronTimeZone.IST))
                                                                        .build();
                                                        return taskDefinitionWriter.updateTaskSchedule(
                                                                taskId, taskSchedule, clientUser, cronTrigger);
                                                    } else {
                                                        return taskDefinitionWriter.updateTaskSchedule(
                                                                taskId, taskSchedule, clientUser);
                                                    }
                                                })
                                        .doOnSuccess(
                                                ignored ->
                                                        createAuditLogForTaskScheduleUpdate(taskId, task, request, clientUser)))
                .doOnError(err -> log.error("task Update failed", err))
                .ignoreElement();
    }

    private void createAuditLogForTaskScheduleUpdate(
            Long taskId,
            TaskWithCohortInfo taskWithCohortInfo,
            UpdateTaskRequest request,
            String userEmail) {
        if (request.getCronExpression() != null) {
            auditLogService.create(
                    AuditLog.builder()
                            .cohortId(taskWithCohortInfo.getCohortId())
                            .taskId(taskId)
                            .action(AuditLogAction.CRON_UPDATED)
                            .value(
                                    new AuditLogValue(
                                            taskWithCohortInfo.getCronExpression(), request.getCronExpression()))
                            .createdBy(userEmail)
                            .build());
        }
        if (request.getEndDate() != null) {
            auditLogService.create(
                    AuditLog.builder()
                            .cohortId(taskWithCohortInfo.getCohortId())
                            .taskId(taskId)
                            .action(AuditLogAction.END_DATE_UPDATED)
                            .value(
                                    new AuditLogValue(
                                            taskWithCohortInfo.getEndTime() * 1000, request.getEndDate() * 1000))
                            .createdBy(userEmail)
                            .build());
        }
        if (request.getStartDate() != null) {
            auditLogService.create(
                    AuditLog.builder()
                            .cohortId(taskWithCohortInfo.getCohortId())
                            .taskId(taskId)
                            .action(AuditLogAction.START_DATE_UPDATED)
                            .value(
                                    new AuditLogValue(
                                            taskWithCohortInfo.getStartTime() * 1000, request.getStartDate() * 1000))
                            .createdBy(userEmail)
                            .build());
        }
    }

    @Override
    public Completable deleteTask(Long taskId, String client, String clientUser) {
      return taskDefinitionWriter
              .deleteDraft(taskId)
              .filter(success -> success)
              .switchIfEmpty(Single.error(new DefinedException(ErrorEntity.NO_SUCH_TASK_DRAFT)))
              .ignoreElement();
    }
}
