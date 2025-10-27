package com.ascend.flockr.service.web;

import com.ascend.flockr.model.task.constant.TaskStatus;
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
