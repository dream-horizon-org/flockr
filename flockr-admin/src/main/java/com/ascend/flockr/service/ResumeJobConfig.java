package com.ascend.flockr.service;

import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.service.web.JobConfig;
import lombok.Getter;

@Getter
public class ResumeJobConfig extends JobConfig {

  private boolean resumeUsingLatestArtifact;
  private boolean resumeFromSavepoint;

  private TaskStatus taskStatus;

  public ResumeJobConfig(
      String clientUser,
      boolean resumeUsingLatestArtifact,
      boolean resumeFromSavepoint,
      TaskStatus taskStatus) {
    super(clientUser);
    this.resumeUsingLatestArtifact = resumeUsingLatestArtifact;
    this.resumeFromSavepoint = resumeFromSavepoint;
    this.taskStatus = taskStatus;
  }
}
