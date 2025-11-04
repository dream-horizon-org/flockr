package com.ascend.flockr.dao.taskdefinition;

import com.ascend.flockr.dao.AbstractRepository;
import com.ascend.flockr.dao.TaskDefinitionReader;
import com.ascend.flockr.model.task.Task;
import com.ascend.flockr.model.task.TaskDefinition;
import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.reactivex.mysqlclient.MySQLPool;
import io.vertx.reactivex.sqlclient.Tuple;
import java.util.List;
import java.util.stream.Collectors;

class TaskDefinitionSlaveRepository extends AbstractRepository implements TaskDefinitionReader {

  private static final String FIND_TASK_DETAIL_BY_ID =
      "SELECT task_id, cohort_id, name, description, type, rule_type, rule_action, UNIX_TIMESTAMP (start_time) AS start_time, UNIX_TIMESTAMP (end_time) AS end_time, "
          + "cron_expression, status, client, UNIX_TIMESTAMP (created_at) AS created_at, created_by, sequence, rule, UNIX_TIMESTAMP (updated_at) AS updated_at, "
          + "updated_by, resume_using_latest_artifact, resume_from_savepoint FROM task_definition WHERE task_id = ?";
  private static final String FIND_STATUS_BY_ID =
      "SELECT status FROM task_definition WHERE task_id = ?";

  private static final String FIND_ALL_TASK_DETAIL_BY_COHORT_ID =
      "SELECT task_id, cohort_id, name, description, type, rule_type, rule_action, UNIX_TIMESTAMP (start_time) AS start_time, UNIX_TIMESTAMP (end_time) AS end_time, "
          + "cron_expression, status, client, UNIX_TIMESTAMP (created_at) AS created_at, created_by, sequence, rule, UNIX_TIMESTAMP (updated_at) AS updated_at, "
          + "updated_by, resume_using_latest_artifact, resume_from_savepoint FROM task_definition WHERE cohort_id = ?";

  private static final String FIND_TASK_BY_ID =
      "SELECT task_id, cohort_id, name, description, type, rule_type, rule_action, UNIX_TIMESTAMP (start_time) AS start_time, "
          + "UNIX_TIMESTAMP (end_time) AS end_time, cron_expression, status, client, UNIX_TIMESTAMP (created_at) AS created_at, created_by "
          + "FROM task_definition WHERE task_id = ?";

  private static final String FIND_ALL_WITH_LATEST_JOB_BY_STATUS_AND_TYPE =
      "SELECT temp.task_id, temp.name, temp.type, temp.rule_action, temp.cron_expression, temp.status, UNIX_TIMESTAMP (temp.updated_at) AS updated_at, "
          + "UNIX_TIMESTAMP (temp.job_created_at) AS job_created_at, jel.id AS job_id, jel.state AS job_status, UNIX_TIMESTAMP (jel.updated_at) AS job_updated_at FROM "
          + "( SELECT td.task_id, td.name, td.type, td.rule_action, td.cron_expression, td.status, td.updated_at, Max(jel.created_at) AS job_created_at "
          + "FROM task_definition AS td INNER JOIN job_execution_log AS jel ON td.task_id = jel.task_id AND td.status IN (%s) "
          + "AND td.type = ? AND TIMESTAMPDIFF(SECOND, td.updated_at, NOW()) > 30 GROUP BY td.task_id"
          + ") AS temp INNER JOIN job_execution_log AS jel ON temp.task_id = jel.task_id AND temp.job_created_at = jel.created_at";

  private static final String FIND_ALL_BY_COHORT_ID_OWNER =
      "SELECT EXISTS(SELECT 1 FROM cohort_owner WHERE cohort_id = ? AND owner = ? AND is_removed = FALSE) AS is_owner ";

    private static final String FIND_TASK_WITH_COHORT_BY_TASK_ID =
            "SELECT td.task_id, td.cohort_id, td.type, UNIX_TIMESTAMP(td.start_time) AS start_time, "
                    + "UNIX_TIMESTAMP(td.end_time) AS end_time, td.cron_expression, td.status, cm.is_expired as is_expired, UNIX_TIMESTAMP(cm.expiration_date) "
                    + "as expiration_date FROM task_definition td INNER JOIN cohort_master cm ON td.cohort_id = cm.id WHERE task_id = ? ";

  protected final TaskDefinitionMapper taskDefinitionMapper = new TaskDefinitionMapper();
  protected final TaskWithJobInfoMapper taskWithJobInfoMapper = new TaskWithJobInfoMapper();
  protected final TaskMapper taskMapper = new TaskMapper();
  protected final TaskWithCohortInfoMapper taskWithCohortInfoMapper = new TaskWithCohortInfoMapper();

    @Inject
  public TaskDefinitionSlaveRepository(@Named("mysql-client-nucleus") MySQLPool client) {
    super(client);
  }

  @Override
  public Maybe<TaskDefinition> findDetailById(Long id) {
    return findOne(FIND_TASK_DETAIL_BY_ID, Tuple.of(id), taskDefinitionMapper);
  }

  @Override
  public Maybe<TaskStatus> findStatusById(Long id) {
    return findOne(
        FIND_STATUS_BY_ID, Tuple.of(id), row -> TaskStatus.valueOf(row.getString("status")));
  }

  @Override
  public Single<List<TaskDefinition>> findAllByCohortId(Long cohortId) {
    return findMultiple(
        FIND_ALL_TASK_DETAIL_BY_COHORT_ID, Tuple.of(cohortId), taskDefinitionMapper);
  }

  @Override
  public Maybe<Task> findById(Long id) {
    return findOne(FIND_TASK_BY_ID, Tuple.of(id), taskMapper);
  }

  @Override
  public Single<List<TaskWithJobInfo>> findAllWithLatestJobByStatusAndType(
      List<TaskStatus> statusList, TaskType type) {
    String commaSepStatusValues =
        statusList.stream()
            .map(TaskStatus::name)
            .map(i -> "'" + i + "'")
            .collect(Collectors.joining(","));
    return findMultiple(
        String.format(FIND_ALL_WITH_LATEST_JOB_BY_STATUS_AND_TYPE, commaSepStatusValues),
        Tuple.of(type),
        taskWithJobInfoMapper);
  }

  @Override
  public Maybe<Boolean> findIfCohortOwnerExists(Long cohortId, String owner) {
    return findOne(
        FIND_ALL_BY_COHORT_ID_OWNER, Tuple.of(cohortId, owner), row -> row.getBoolean("is_owner"));
  }

    @Override
    public Maybe<TaskWithCohortInfo> findTaskWithCohortInfoById(Long taskId) {
        return findOne(FIND_TASK_WITH_COHORT_BY_TASK_ID, Tuple.of(taskId), taskWithCohortInfoMapper);
    }
}
