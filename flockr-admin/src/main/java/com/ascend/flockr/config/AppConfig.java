package com.ascend.flockr.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class AppConfig {

  private FlinkConfig flink;
  private RuleConfig rule;

  private VertxTimerConfig timer;

  @JsonProperty("realtimejobshealth")
  private VertxCronConfig realTimeJobsHealth;

  @JsonProperty("historicjobshealth")
  private VertxCronConfig historicJobsHealth;

  @JsonProperty("transientstatemetricpush")
  private VertxCronConfig transientStateMetricPush;

  @JsonProperty("executescheduledjobs")
  private VertxCronConfig executeScheduledJobs;

  @JsonProperty("terminateexpiredjobs")
  private VertxCronConfig terminateExpiredJobs;

  @JsonProperty("updatecohortexpiry")
  private VertxCronConfig updateCohortExpiry;

  @JsonProperty("eventnameregister")
  private VertxCronConfig eventNameRegister;

  @JsonProperty("executecrontriggers")
  private VertxCronConfig executeCronTriggers;

  @JsonProperty("icebergingestion")
  private VertxCronConfig icbrgIngestion;

  @JsonProperty("icebergsla")
  private VertxCronConfig icbrgSLA;

  @JsonProperty("slacknotification")
  private VertxCronConfig slackNotification;

  @JsonProperty("countjob")
  private VertxCronConfig countJob;

  @JsonProperty("longrunningjob")
  private VertxCronConfig longRunningJob;

  @JsonProperty("longrunningjobthresholdminute")
  private Long longRunningJobThresholdTimeInMinutes;

  private AuthConfig auth;
  private DataTitanConfig datatitan;
  private SlackConfig slack;
  private MsdConfig msd;

  @JsonProperty("usercohort")
  private UserCohortConfig userCohort;

  @JsonProperty("comaservice")
  private ComaServiceConfig comaService;

  @JsonProperty("clevertap")
  private CleverTapConfig clevertap;

  @JsonProperty("cohortcount")
  private CohortCountConsumer cohortCount;

  @JsonProperty("mark7")
  private Mark7Config mark7;

  @JsonProperty("mark7jobshealth")
  private VertxCronConfig mark7JobHealth;

  @JsonProperty("inactivetopicdeletion")
  private VertxCronConfig inactiveTopicDeletion;
}
