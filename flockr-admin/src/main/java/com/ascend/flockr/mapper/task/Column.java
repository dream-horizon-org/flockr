package com.ascend.flockr.mapper.task;

public interface Column {

  String ID = "task_id";
  String COHORT_ID = "cohort_id";
  String NAME = "name";
  String DESCRIPTION = "description";
  String START_TIME = "start_time";
  String END_TIME = "end_time";
  String CRON_EXPRESSION = "cron_expression";
  String TYPE = "type";
  String RULE_TYPE = "rule_type";
  String RULE_ACTION = "rule_action";
  String SEQUENCE = "sequence";
  String RULE = "rule";
  String STATUS = "status";
  String CLIENT = "client";
  String CREATED_AT = "created_at";
  String CREATED_BY = "created_by";
  String UPDATED_AT = "updated_at";
  String UPDATED_BY = "updated_by";
  String RESUME_USING_LATEST_ARTIFACT = "resume_using_latest_artifact";
  String RESUME_FROM_SAVEPOINT = "resume_from_savepoint";
}

interface CronTriggerColumn {
  String ID = "id";
  String TASK_ID = "task_id";
  String STATUS = "status";
  String REQUEST_ID = "request_id";
  String EXECUTION_ID = "execution_id";
  String CRON_EXPRESSION = "cron_expression";
  String NEXT_EXECUTION_TIME = "next_execution_time";
  String CREATED_AT = "created_at";
  String UPDATED_AT = "updated_at";
}

interface ALIAS {

  String JOB_ID = "job_id";
  String JOB_CREATED_AT = "job_created_at";
  String JOB_UPDATED_AT = "job_updated_at";
  String JOB_STATE = "job_status";
  String TASK_NAME = "task_name";
  String COHORT_NAME = "cohort_name";
  String OWNER = "owner";
}
