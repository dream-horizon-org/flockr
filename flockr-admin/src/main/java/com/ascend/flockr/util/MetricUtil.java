package com.ascend.flockr.util;

public class MetricUtil {

  private static final String SERVICE_NAME = "nucleus_admin";
  private static final String SEPARATOR = "_";

  protected static final String TASK_FAILED = "TASK_FAILED";
  protected static final String JOB_EXECUTION_FAILED = "JOB_EXECUTION_FAILED";
  protected static final String JOB_EXECUTION_NOTIFICATION = "JOB_EXECUTION_NOTIFICATION";
  protected static final String ICEBERG_INGESTION_SLA_BREACHED = "ICEBERG_INGESTION_SLA_BREACHED";
  protected static final String COHORT_USER_COUNT = "COHORT_USER_COUNT_EVENT";

  public static String aspect(String... suffix) {
    StringBuilder aspect = new StringBuilder(SERVICE_NAME);
    for (String phrase : suffix) {
      aspect.append(SEPARATOR).append(phrase.toLowerCase());
    }

    return aspect.toString();
  }

  public static String taskFailedAspect() {
    return aspect(TASK_FAILED);
  }

  public static String executionFailedAspect() {
    return aspect(JOB_EXECUTION_FAILED);
  }

  public static String jobExecutionNotificationAspect() {
    return aspect(JOB_EXECUTION_NOTIFICATION);
  }

  public static String icebergIngestionBreachAspect() {
    return aspect(ICEBERG_INGESTION_SLA_BREACHED);
  }

  public static String cohortUserCountEventAspect() {
    return aspect(COHORT_USER_COUNT);
  }
}
