package com.ascend.flockr.domain.task;

import com.ascend.flockr.domain.rule.Rule;
import lombok.Data;

@Data
public class TaskDefinition extends Task {

  //    private AbstractSequence sequence;
  private Rule rule;
  private Long updatedAt; /* epoch seconds */
  private String updatedBy;
  private Boolean resumeUsingLatestArtifact;
  private Boolean resumeFromSavepoint;
}
