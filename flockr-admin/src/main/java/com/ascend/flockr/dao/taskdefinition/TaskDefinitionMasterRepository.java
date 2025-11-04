package com.ascend.flockr.dao.taskdefinition;

import com.ascend.flockr.dao.TaskDefinitionWriter;
import com.ascend.flockr.model.crontrigger.CronTrigger;
import com.ascend.flockr.model.crontrigger.CronTriggerStatus;
import com.ascend.flockr.model.task.TaskDefinition;
import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import com.ascend.flockr.util.JsonColumnUtil;
import com.ascend.flockr.util.MetricUtil;
import com.timgroup.statsd.StatsDClient;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskDefinitionMasterRepository extends TaskDefinitionSlaveRepository
        implements TaskDefinitionWriter {

    private static final String DELETE_DRAFT =
            "DELETE FROM task_definition WHERE task_id = ? AND status = 'Draft'";

    private static final String UPDATE_TASK = "UPDATE task_definition SET %s WHERE task_id = ?";

    private static final String UPDATE_TASK_END_DATE_AND_STATUS =
            "UPDATE task_definition SET end_time = FROM_UNIXTIME (?), status = ?, updated_by = ? WHERE task_id = ?";

    private static final String UPDATE_STATUS =
            "UPDATE task_definition SET status = ? WHERE task_id = ?";

    private static final String UPDATE_TASK_STATUS_AND_UPDATED_BY =
            "UPDATE task_definition SET status = ?, updated_by = ? WHERE task_id = ?";

    private static final String UPDATE_TASK_STATUS_BY_ID_AND_UPDATED_AT =
            "UPDATE task_definition SET status = ? WHERE task_id = ? AND updated_at = FROM_UNIXTIME(?)";

    private static final String INSERT_TASK =
            "INSERT INTO task_definition (cohort_id, name, description, start_time, end_time, cron_expression, type, rule_type, rule_action, sequence, rule, "
                    + "status, client, created_by, updated_by) VALUES (?, ?, ?, FROM_UNIXTIME (?), FROM_UNIXTIME (?), ?, ?, ?, ?, ?,?, ?, ?, ?, ?)";

    private static final String UPDATE_CRON_STATUS_BY_TASK_ID_AND_STATUS =
            "UPDATE cron_triggers SET status = ? WHERE task_id = ? AND status = ?";

    private static final String INSERT_CRON =
            "INSERT INTO cron_triggers(request_id, status, task_id, cron_expression, execution_id, next_execution_time) VALUES (?, ?, ?, ?, ?, FROM_UNIXTIME (?))";


    private Tuple taskDefinitionTuple(TaskDefinition taskDefinition) {
        Tuple tuple = Tuple.tuple();
        tuple.addValue(taskDefinition.getCohortId());
        tuple.addValue(taskDefinition.getName());
        tuple.addValue(taskDefinition.getDescription());
        tuple.addValue(taskDefinition.getStartTime());
        tuple.addValue(taskDefinition.getEndTime());
        tuple.addValue(taskDefinition.getCronExpression());
        tuple.addValue(taskDefinition.getType());
        tuple.addValue(taskDefinition.getRuleType());
        tuple.addValue(taskDefinition.getAction());
        tuple.addValue(JsonColumnUtil.set(taskDefinition.getSequence()));
        tuple.addValue(JsonColumnUtil.set(taskDefinition.getRule()));
        tuple.addValue(taskDefinition.getStatus());
        tuple.addValue(taskDefinition.getClient());
        tuple.addValue(taskDefinition.getCreatedBy());
        tuple.addValue(taskDefinition.getUpdatedBy());
        return tuple;
    }

    @Override
    public Single<Boolean> deleteDraft(Long id) {
        return delete(DELETE_DRAFT, Tuple.of(id)).map(rowsAffected -> rowsAffected > 0);
    }

    @Override
    public Single<Long> create(TaskDefinition taskDefinition) {
        Tuple tuple = taskDefinitionTuple(taskDefinition);
        return insertAndGenerateId(INSERT_TASK, tuple);
    }

    @Override
    public Single<Boolean> updateTaskSchedule(
            Long taskId, TaskSchedule taskSchedule, String updatedBy) {
        Tuple tuple = Tuple.tuple();
        StringBuilder setClause = new StringBuilder();
        if (taskSchedule.getStartDate() != null) {
            setClause.append(Column.START_TIME).append("= FROM_UNIXTIME (?), ");
            tuple.addValue(taskSchedule.getStartDate());
        }
        if (taskSchedule.getEndDate() != null) {
            setClause.append(Column.END_TIME).append("= FROM_UNIXTIME (?), ");
            tuple.addValue(taskSchedule.getEndDate());
        }
        if (taskSchedule.getCronExpression() != null) {
            setClause.append(Column.CRON_EXPRESSION).append("= ?, ");
            tuple.addValue(taskSchedule.getCronExpression());
        }
        setClause.append(Column.UPDATED_BY).append("= ?");
        tuple.addValue(updatedBy);

        tuple.addValue(taskId);
        return update(String.format(UPDATE_TASK, setClause), tuple)
                .map(rowsAffected -> rowsAffected > 0);
    }

    @Override
    public Single<Boolean> updateEndDateAndStatus(
            Long id, TaskType type, TaskStatus status, String updatedBy, Long endDateInSeconds) {
        return update(
                UPDATE_TASK_END_DATE_AND_STATUS, Tuple.of(endDateInSeconds, status, updatedBy, id))
                .map(rowsAffected -> rowsAffected > 0)
                .doOnSuccess(
                        success -> {
                            if (success && status == TaskStatus.Failed) {
                                incrementFailureCounter(id, type);
                            }
                        });
    }

    @Override
    public Single<Boolean> updateStatus(Long id, TaskType type, TaskStatus status) {
        return update(UPDATE_STATUS, Tuple.of(status, id))
                .map(rowsAffected -> rowsAffected > 0)
                .doOnSuccess(
                        success -> {
                            if (success && status == TaskStatus.Failed) {
                                incrementFailureCounter(id, type);
                            }
                        });
    }

    @Override
    public Single<Boolean> updateStatusAndUpdatedBy(
            Long id, TaskType type, TaskStatus status, String updatedBy) {
        return update(UPDATE_TASK_STATUS_AND_UPDATED_BY, Tuple.of(status, updatedBy, id))
                .map(rowsAffected -> rowsAffected > 0)
                .doOnSuccess(
                        success -> {
                            if (success && status == TaskStatus.Failed) {
                                incrementFailureCounter(id, type);
                            }
                        });
    }

    @Override
    public Single<Boolean> updateStatusByIdAndUpdatedAt(
            Long id, TaskType type, TaskStatus status, Long updatedAtInSeconds) {
        return update(UPDATE_TASK_STATUS_BY_ID_AND_UPDATED_AT, Tuple.of(status, id, updatedAtInSeconds))
                .map(rowsAffected -> rowsAffected > 0)
                .doOnSuccess(
                        success -> {
                            if (success && status == TaskStatus.Failed) {
                                incrementFailureCounter(id, type);
                            }
                        });
    }

    @Override
    public Single<Boolean> updateTaskSchedule(
            Long taskId, TaskSchedule taskSchedule, String updatedBy, CronTrigger cronTrigger) {
        return client()
                .rxBegin()
                .doOnSuccess(transaction -> log.info("UTS: Transaction Begin"))
                .doOnError(err -> log.error("UTS: Transaction Begin Failed"))
                .flatMap(
                        transaction ->
                                transaction
                                        .preparedQuery(UPDATE_CRON_STATUS_BY_TASK_ID_AND_STATUS)
                                        .rxExecute(
                                                Tuple.of(CronTriggerStatus.INACTIVE, taskId, CronTriggerStatus.ACTIVE))
                                        .doOnSuccess(id -> log.info("CT: cron deactivated for taskId: {}", taskId))
                                        .doOnError(
                                                id -> log.error("CT: cohort deactivation failed for taskId - {}", taskId))
                                        .flatMap(
                                                ignored ->
                                                        transaction
                                                                .preparedQuery(INSERT_CRON)
                                                                .rxExecute(createCronTriggerTuple(cronTrigger))
                                                                .doOnSuccess(
                                                                        id -> log.info("CT: cron activated  for taskId: {}", taskId))
                                                                .doOnError(
                                                                        id ->
                                                                                log.error(
                                                                                        "CT: cron activation failed for taskId - {}", taskId))
                                                                .flatMap(
                                                                        ignored1 -> {
                                                                            Tuple tuple = Tuple.tuple();
                                                                            StringBuilder setClause = new StringBuilder();
                                                                            if (taskSchedule.getStartDate() != null) {
                                                                                setClause
                                                                                        .append(Column.START_TIME)
                                                                                        .append("= FROM_UNIXTIME (?), ");
                                                                                tuple.addValue(taskSchedule.getStartDate());
                                                                            }
                                                                            if (taskSchedule.getEndDate() != null) {
                                                                                setClause
                                                                                        .append(Column.END_TIME)
                                                                                        .append("= FROM_UNIXTIME (?), ");
                                                                                tuple.addValue(taskSchedule.getEndDate());
                                                                            }
                                                                            if (taskSchedule.getCronExpression() != null) {
                                                                                setClause.append(Column.CRON_EXPRESSION).append("= ?, ");
                                                                                tuple.addValue(taskSchedule.getCronExpression());
                                                                            }
                                                                            setClause.append(Column.UPDATED_BY).append("= ?");
                                                                            tuple.addValue(updatedBy);

                                                                            tuple.addValue(taskId);
                                                                            return transaction
                                                                                    .preparedQuery(String.format(UPDATE_TASK, setClause))
                                                                                    .rxExecute(tuple)
                                                                                    .map(rows -> rows.rowCount() > 0);
                                                                        }))
                                        .doOnSuccess(
                                                ignored -> {
                                                    transaction.commit();
                                                    log.info("UTS: Transaction Commit");
                                                })
                                        .doOnError(
                                                err -> {
                                                    log.error(
                                                            "Error occurred during update task schedule transaction, rolling back"
                                                    );
                                                    transaction.close();
                                                    log.info("UTS: Transaction Close");
                                                }));
    }


    private void incrementFailureCounter(Long taskId, TaskType taskType) {
        log.warn("Task failure recorded. task_id={}, task_type={}", taskId, taskType);
    }

    private Tuple createCronTriggerTuple(CronTrigger cronTrigger) {

        Tuple tuple = Tuple.tuple();
        tuple.addValue(cronTrigger.getRequestId());
        tuple.addValue(cronTrigger.getStatus());
        tuple.addValue(cronTrigger.getTaskId());
        tuple.addValue(cronTrigger.getCronExpression());
        tuple.addValue(1);
        tuple.addValue(cronTrigger.getNextExecutionTime());
        return tuple;
    }
}
