package com.ascend.flockr.repository.sql;

public final class TaskSql {

  public static final String FIND_BY_ID =
      "SELECT task_id, cohort_id, name, description, type, rule_type, rule_action, "
          + "UNIX_TIMESTAMP(start_time) AS start_time, UNIX_TIMESTAMP(end_time) AS end_time, "
          + "cron_expression, status, client, UNIX_TIMESTAMP(created_at) AS created_at, created_by, "
          + "sequence, rule, UNIX_TIMESTAMP(updated_at) AS updated_at, updated_by, "
          + "resume_using_latest_artifact, resume_from_savepoint "
          + "FROM task_definition WHERE task_id = ?";

  public static final String INSERT_TASK =
      "INSERT INTO task_definition (cohort_id, name, description, type, rule_type, rule_action, "
          + "start_time, end_time, cron_expression, status, client, created_by, sequence, rule, "
          + "updated_by, resume_using_latest_artifact, resume_from_savepoint) "
          + "VALUES (?, ?, ?, ?, ?, ?, FROM_UNIXTIME(?), FROM_UNIXTIME(?), ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private TaskSql() {}
}
