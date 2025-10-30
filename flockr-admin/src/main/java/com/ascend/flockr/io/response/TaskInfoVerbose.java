package com.ascend.flockr.io.response;

import com.ascend.flockr.io.request.AbstractSequence;
import com.ascend.flockr.model.task.rule.Rule;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskInfoVerbose extends TaskInfo {

  private AbstractSequence sequence;
  private Rule rule;
  private String updatedAt;
  private String updatedBy;
  private Boolean resumeUsingLatestArtifact;
  private Boolean resumeFromSavepoint;
}
