package com.ascend.flockr.exception;

import com.ascend.flockr.model.Error;
import lombok.Getter;

@Getter
public enum ErrorEntity {
  USER_UNAUTHORIZED(
      "user info is not available", Error.of("AUTHENTICATION_FAILURE", "user not authorized"), 401),

  INCORRECT_DATA_TYPE(
      "condition value data type invalid for {eventName: %s, propertyName: %s}",
      Error.of("UNPROCESSABLE_ENTITY", "condition value data type invalid"), 422),

  INVALID_GROUP_BY(
      "Invalid GroupBy", Error.of("UNPROCESSABLE_ENTITY", "userId is not present in groupBy"), 422),

  NO_ROWS_MATCHED(
      "no rows matched in update operation",
      Error.of("UNPROCESSABLE_ENTITY", "either taskId is invalid or task is not editable"),
      422),

  NO_SUCH_TASK(
      "no task exists for input taskId",
      Error.of("UNPROCESSABLE_ENTITY", "no task exists for input taskId"),
      422),

  NO_SUCH_TASK_DRAFT(
      "no task draft exists for input taskId",
      Error.of("UNPROCESSABLE_ENTITY", "no task draft exists for input taskId"),
      422),

  END_DATE_NOT_EDITABLE(
      "end date is not editable for task which is not active",
      Error.of("UNPROCESSABLE_ENTITY", "end date is not editable for task which is not active"),
      422),

  TASK_NOT_RUNNING(
      "task is not in running state",
      Error.of("UNPROCESSABLE_ENTITY", "task is not in running state"),
      422),

  TASK_NOT_PAUSED(
      "task is not in paused state",
      Error.of("UNPROCESSABLE_ENTITY", "task is not in paused state"),
      422),

  TASK_NOT_RESUMABLE(
      "task is not in resumable state",
      Error.of("UNPROCESSABLE_ENTITY", "task is not in resumable state"),
      422),

  TASK_NOT_TERMINABLE(
      "task is not in terminable state",
      Error.of("UNPROCESSABLE_ENTITY", "task is not in terminable state"),
      422),

  TASK_NOT_ELIGIBLE_FOR_MAINTENANCE(
      "task is not eligible for maintenance",
      Error.of("UNPROCESSABLE_ENTITY", "task is not eligible for maintenance"),
      422),

  OPERATION_NOT_SUPPORTED(
      "specified operation could not be performed on task",
      Error.of("METHOD_NOT_ALLOWED", "specified operation could not be performed on task"),
      405),

  OPERATION_NOT_SUPPORTED_TEMPORARILY(
      "can not have more than 1 cohorts filter in belongsTo",
      Error.of("METHOD_NOT_ALLOWED", "operation is not supported temporarily"),
      405),

  RULE_VALIDATION_WITH_PROVIDER_FAILED(
      "rule validation failed", Error.of("FAILED_DEPENDENCY", "rule validation failed"), 424),

  MULTIPLE_RULE_WITH_SAME_NAME_NOT_ALLOWED(
      "uniqueness constraint imposed by task",
      Error.of("FAILED_DEPENDENCY", "multiple rules are not allowed with same name for a cohort"),
      424),

  MULTIPLE_COHORT_NOT_ALLOWED(
      "uniqueness constraint imposed by cohort",
      Error.of("FAILED_DEPENDENCY", "Cohort with same name already exist"),
      424),

  MULTIPLE_RULE_ALLOWED(
      "multiple rules are allowed, name uniqueness could not be imposed, for input task name",
      Error.of(
          "FAILED_DEPENDENCY",
          "multiple rules are allowed, name uniqueness could not be imposed, for input task name"),
      424),

  AUDIENCE_NAME_MISMATCH(
      "audience name mismatch", Error.of("UNPROCESSABLE_ENTITY", "audience name mismatch"), 422),

  PARSING_FAILED("%s", Error.of("INTERNAL_SERVER_ERROR", "PARSING FAILED"), 500),

  INVALID_EXPIRATION_DATE(
      "invalid expiry date", Error.of("UNPROCESSABLE_ENTITY", "expiry date is not valid"), 422),

  INVALID_END_DATE(
      "Invalid end date for task: %s",
      Error.of(
          "UNPROCESSABLE_ENTITY",
          "Task end date must be less than or equals to cohort expiry date and greater than or equals to task start date"),
      422),

  INVALID_START_DATE(
      "Invalid start date for task: %s",
      Error.of("UNPROCESSABLE_ENTITY", "Invalid start date"), 422),

  USER_ID_NOT_FOUND_IN_CT_DESTINATION_PROPERTIES(
      "userId is not present",
      Error.of("UNPROCESSABLE_ENTITY", "userId is not present in clevertap properties"),
      422),

  CONSTRAINTS_CANNOT_BE_NULL(
      "constraint is not present",
      Error.of("UNPROCESSABLE_ENTITY", "constraint is required for more than 1 event"),
      422),

  QUERY_VALIDATION_FAILED("%s", Error.of("UNPROCESSABLE_ENTITY", "query validation failed"), 422),

  DESTINATION_LIMIT_EXCEEDED_QUERY_VALIDATION(
      "The 'limit' clause cannot be greater than %s cr since %s is selected as a destination",
      Error.of("UNPROCESSABLE_ENTITY", "Destination limit exceeded"), 422),

  DESTINATION_LIMIT_EXCEEDED_RULE_QUERY_VALIDATION(
      "Rule %s has a 'limit' clause exceeding the max value %s cr",
      Error.of("UNPROCESSABLE_ENTITY", "Destination limit exceeded"), 422),

  SELECT_CLAUSE_SINGLE_COLUMN_VALIDATION(
      "only %s column allowed in SELECT clause",
      Error.of("UNPROCESSABLE_ENTITY", "only 1 column allowed in SELECT clause"), 422),

  SELECT_CLAUSE_SINGLE_COLUMN_NAME_VALIDATION(
      "Column %s not in SELECT clause",
      Error.of("UNPROCESSABLE_ENTITY", "Expected Column not in SELECT clause"), 422),

  SELECT_CLAUSE_FROM_VALIDATION(
      "From is not present in SELECT clause",
      Error.of("UNPROCESSABLE_ENTITY", "From is not present in SELECT clause"),
      422),

  SINGLE_SELECT_CLAUSE_VALIDATION(
      "The provided query is not a PlainSelect and is not supported",
      Error.of(
          "UNPROCESSABLE_ENTITY", "The provided query is not a PlainSelect and is not supported"),
      422),

  UNSUPPORTED_SELECT_CLAUSE(
      "Alias not defined for %s",
      Error.of("UNPROCESSABLE_ENTITY", "Alias not defined for SELECT column"), 422),

  UNSUPPORTED_ALL_SELECT("%s", Error.of("UNPROCESSABLE_ENTITY", "SELECT * NOT SUPPORTED"), 422),

  CLEVERTAP_DESTINATION_NOT_SUPPORTED(
      "CleverTap destination NOT SUPPORTED",
      Error.of("UNPROCESSABLE_ENTITY", "CleverTap destination NOT SUPPORTED"),
      422),

  EXPIRATION_DATE_MISMATCH(
      "input expiration date does not match with existing cohort's expiry",
      Error.of(
          "UNPROCESSABLE_ENTITY",
          "input expiration date does not match with existing cohort's expiry"),
      422),

  USER_NOT_AUTHORIZED_TO_ADD_RULE(
      "user is not authorised to add rule for this cohort",
      Error.of("UNAUTHORIZED", "user is not authorised to add rule for this cohort"),
      401),

  NO_SUCH_COHORT(
      "no cohort exists for input cohortId",
      Error.of("UNPROCESSABLE_ENTITY", "no cohort exists for input cohortId"),
      422),

  DESTINATION_NOT_SUPPORTED(
      "%s Destination NOT SUPPORTED",
      Error.of("UNPROCESSABLE_ENTITY", "Destination NOT SUPPORTED"), 422),

  DATA_FEAST_DESTINATION_DEPRECATED(
      "It looks like this cohort is on an old AE architecture! Please post about this on #audience_engine_on_call & we will fix it for you!",
      Error.of(
          "UNPROCESSABLE_ENTITY",
          "It looks like this cohort is on an old AE architecture! Please post about this on #audience_engine_on_call & we will fix it for you!"),
      422),

  ACTION_NOT_SUPPORTED(
      "%s action NOT SUPPORTED", Error.of("UNPROCESSABLE_ENTITY", "action NOT SUPPORTED"), 422),

  DYNAMIC_EXPIRE_UNIT_NOT_SUPPORTED(
      "% unit NOT SUPPORTED",
      Error.of("UNPROCESSABLE_ENTITY", "dynamic expire time unit NOT SUPPORTED"), 422),

  USER_NOT_AUTHORIZED_TO_UPDATE_COHORT(
      "User is not authorised to update this cohort",
      Error.of("UNAUTHORIZED", "User is not authorised to update this cohort"),
      401),

  INVALID_EXPIRATION_DATE_UPDATE(
      "expiry date is not valid",
      Error.of(
          "UNPROCESSABLE_ENTITY", "expiry date should past the existing expiry date of cohort"),
      422),

  COHORT_EXPIRED(
      "cohort is already expired",
      Error.of("UNPROCESSABLE_ENTITY", "cohort is already expired"),
      422),

  EXPIRATION_DATE_UPDATE_FAILED(
      "failed to update cohort expiry date",
      Error.of("UNPROCESSABLE_ENTITY", "failed to update cohort expiry date"),
      500),

  MULTIPLE_RULES_NOT_SUPPORTED(
      "cannot add more than one rule in overwrite mode",
      Error.of("UNPROCESSABLE_ENTITY", "cannot add more than one rule in overwrite mode"),
      422),

  REAL_TIME_RULE_TYPE_NOT_SUPPORTED(
      "real-time rule is not supported for this cohort type",
      Error.of("UNPROCESSABLE_ENTITY", "real-time rule is not supported for this cohort type"),
      422),

  INVALID_REQUEST(
      "request is not valid", Error.of("UNPROCESSABLE_ENTITY", "request is not valid"), 422),

  DYNAMIC_EXPIRE_CONFIG_CANNOT_BE_NULL(
      "time based removal config cannot be null",
      Error.of("INVALID_REQUEST", "time based removal config cannot be null"),
      422),

  CRON_EXPRESSION_CANNOT_BE_NULL(
      "cron expression cannot be null in overwrite mode",
      Error.of("UNPROCESSABLE_ENTITY", "cron expression cannot be null in overwrite mode"),
      422),

  CRON_EXPRESSION_CONSTRAINT_VIOLATION(
      "minimum time difference of 1 hour is required between executions for a cron expression",
      Error.of(
          "UNPROCESSABLE_ENTITY",
          "minimum time difference of 1 hour is required between executions for a cron expression"),
      422),

  MAX_OWNER_LIMIT_REACHED(
      "max 5 owners can be added in this cohort",
      Error.of("UNPROCESSABLE_ENTITY", "max 5 owners can be added in this cohort"),
      422),

  ATLEAST_ONE_OWNER_REQUIRED(
      "at least 1 owner should be present",
      Error.of("UNPROCESSABLE_ENTITY", "at least 1 owner should be present"),
      422),

  OWNER_ALREADY_PRESENT(
      "owner already present", Error.of("UNPROCESSABLE_ENTITY", "owner already present"), 422),

  OWNER_UPDATE_FAILED(
      "failed to update cohort owners",
      Error.of("UNPROCESSABLE_ENTITY", "failed to update cohort owner"),
      500),

  OWNER_NOT_FOUND(
      "specified user is not listed as an owner of this cohort",
      Error.of("UNPROCESSABLE_ENTITY", "specified user is not listed as an owner of this cohort"),
      422),

  USER_NOT_FOUND_FOR_EMAIL(
      "User not found for email",
      Error.of("UNPROCESSABLE_ENTITY", "User not found for email: %s"),
      422),

  UNKNOWN_ERROR_FROM_SLACK_API(
      "Unknown error from slack api",
      Error.of("UNPROCESSABLE_ENTITY", "Unknown error from slack api lookup email: %s"),
      422),

  CRON_NOT_FOUND(
      "cron expression is not present in task",
      Error.of("UNPROCESSABLE_ENTITY", "cron expression is not present in task"),
      422),

  COHORT_NOT_FOUND(
      "Task not associated with cohort",
      Error.of("UNPROCESSABLE_ENTITY", "Task not associated with cohort"),
      422),

  INVALID_RULE_ACTION(
      "Invalid rule action", Error.of("UNPROCESSABLE_ENTITY", "Invalid rule action"), 422),

  OVERWRITE_RULE_NOT_SUPPORTED(
      "Overwrite rule is not yet supported for selected destination",
      Error.of(
          "UNPROCESSABLE_ENTITY", "Overwrite rule is not yet supported for selected destination"),
      422),

  USER_EXPIRY_NOT_SUPPORTED(
      "User expiry not yet supported for this destination",
      Error.of("UNPROCESSABLE_ENTITY", "User expiry not yet supported for this destination"),
      422),

  UPDATE_PARALLELISM_NOT_SUPPORTED(
      "Update parallelism only supported for eventStream rules",
      Error.of("UNPROCESSABLE_ENTITY", "Update parallelism only supported for eventStream rules"),
      422),

  UPDATE_NOTIFY_SLACK_NOT_SUPPORTED(
      "Update notify slack only supported for eventStream rules",
      Error.of("UNPROCESSABLE_ENTITY", "Update notify slack only supported for eventStream rules"),
      422),
  SIMILAR_OPERATION_PERFORMED(
      "Same operation performed", Error.of("METHOD_NOT_ALLOWED", "Same operation performed"), 405),
  OWNER_ADDITION_NOT_ALLOWED(
      "Owner addition not allowed",
      Error.of("METHOD_NOT_ALLOWED", "Some owners present are not designated as verifiers."),
      405),
  ;

  private final String cause;
  private final Error error;
  private final int httpStatusCode;

  ErrorEntity(String cause, Error error, int httpStatusCode) {
    this.cause = cause;
    this.error = error;
    this.httpStatusCode = httpStatusCode;
  }
}
