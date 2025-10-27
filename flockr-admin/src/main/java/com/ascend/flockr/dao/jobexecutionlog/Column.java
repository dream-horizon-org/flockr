package com.ascend.flockr.dao.jobexecutionlog;

interface Column {

  String ID = "id";
  String TASK_ID = "task_id";
  String JAR_ID = "jar_id";
  String START_TIME = "start_time";
  String END_TIME = "end_time";
  String STATE = "state";
  String CREATED_AT = "created_at";
  String UPDATED_AT = "updated_at";
  String REQUEST_ID = "request_id";
  String SYNCED = "synced";
  String WATERMARK = "watermark";
  String ERROR_LOG = "error_log";
  String RECORDS_PROCESSED = "records_processed";
}
